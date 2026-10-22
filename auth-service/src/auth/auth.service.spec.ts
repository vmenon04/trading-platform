import { ConflictException, UnauthorizedException } from "@nestjs/common";
import * as bcrypt from "bcrypt";
import * as jwt from "jsonwebtoken";
import { createHash } from "node:crypto";
import { AuthService } from "./auth.service";
import { TokenRepository, RefreshTokenRecord } from "./token.repository";
import { UserRecord, UserRepository } from "./user.repository";

jest.mock("bcrypt", () => ({
    hash: jest.fn(),
    compare: jest.fn(),
}));

describe("AuthService", () => {
    let service: AuthService;
    let userRepository: jest.Mocked<UserRepository>;
    let tokenRepository: jest.Mocked<TokenRepository>;

    const existingUser: UserRecord = {
        userId: 42n,
        externalUserId: "6af4a0ef-6421-401b-a39b-b2ba0ee37afd",
        username: "alice",
        passwordHash: "$2b$10$hashedPassword",
        clientId: 10n,
        roles: ["CLIENT"],
        createdAt: new Date("2026-01-01T00:00:00.000Z"),
    };

    beforeEach(() => {
        userRepository = {
            existsByUsername: jest.fn(),
            createUser: jest.fn(),
            findByUsername: jest.fn(),
            findById: jest.fn(),
            updatePasswordHash: jest.fn(),
            updateClientId: jest.fn(),
            replaceRoles: jest.fn(),
            addRole: jest.fn(),
            removeRole: jest.fn(),
            onModuleDestroy: jest.fn(),
        } as unknown as jest.Mocked<UserRepository>;

        tokenRepository = {
            createRefreshToken: jest.fn(),
            findActiveByHash: jest.fn(),
            findById: jest.fn(),
            revokeById: jest.fn(),
            revokeByHash: jest.fn(),
            revokeAllForUser: jest.fn(),
            replaceToken: jest.fn(),
            onModuleDestroy: jest.fn(),
        } as unknown as jest.Mocked<TokenRepository>;

        service = new AuthService(userRepository, tokenRepository);
        process.env.JWT_SECRET = "test-secret";

        (bcrypt.hash as jest.Mock).mockReset();
        (bcrypt.compare as jest.Mock).mockReset();
    });

    afterEach(() => {
        jest.restoreAllMocks();
        delete process.env.JWT_SECRET;
    });

    it("register creates a user with hashed password", async () => {
        userRepository.existsByUsername.mockResolvedValue(false);
        (bcrypt.hash as jest.Mock).mockResolvedValue("hashed-pass");

        const result = await service.register("alice", "plain-pass", ["CLIENT"]);

        expect(result).toEqual({ username: "alice", registered: true });
        expect(userRepository.createUser).toHaveBeenCalledWith({
            username: "alice",
            passwordHash: "hashed-pass",
            roles: ["CLIENT"],
        });
    });

    it("register rejects duplicate usernames", async () => {
        userRepository.existsByUsername.mockResolvedValue(true);

        await expect(service.register("alice", "plain-pass")).rejects.toBeInstanceOf(ConflictException);
        expect(userRepository.createUser).not.toHaveBeenCalled();
    });

    it("login rejects unknown user", async () => {
        userRepository.findByUsername.mockResolvedValue(null);

        await expect(service.login("alice", "plain-pass")).rejects.toBeInstanceOf(UnauthorizedException);
        expect(tokenRepository.createRefreshToken).not.toHaveBeenCalled();
    });

    it("login rejects wrong password", async () => {
        userRepository.findByUsername.mockResolvedValue(existingUser);
        (bcrypt.compare as jest.Mock).mockResolvedValue(false);

        await expect(service.login("alice", "wrong-pass")).rejects.toBeInstanceOf(UnauthorizedException);
        expect(tokenRepository.createRefreshToken).not.toHaveBeenCalled();
    });

    it("login issues access and refresh tokens for valid credentials", async () => {
        userRepository.findByUsername.mockResolvedValue(existingUser);
        (bcrypt.compare as jest.Mock).mockResolvedValue(true);
        tokenRepository.createRefreshToken.mockResolvedValue({
            tokenId: 1,
            userId: existingUser.userId,
            tokenHash: "hash",
            expiresAt: new Date(Date.now() + 1000),
            revokedAt: null,
            createdAt: new Date(),
        } as RefreshTokenRecord);

        const result = await service.login("alice", "plain-pass");

        expect(typeof result.accessToken).toBe("string");
        expect(result.refreshToken).toHaveLength(64);

        const decoded = jwt.verify(result.accessToken, "test-secret") as jwt.JwtPayload;
        expect(decoded.sub).toBe("42");
        expect(decoded.roles).toEqual(["CLIENT"]);

        expect(tokenRepository.createRefreshToken).toHaveBeenCalledWith(
            existingUser.userId,
            createHash("sha256").update(result.refreshToken).digest("hex"),
            expect.any(Date),
        );
    });

    it("refresh rejects invalid or expired refresh token", async () => {
        tokenRepository.findActiveByHash.mockResolvedValue(null);

        await expect(service.refresh("invalid-token")).rejects.toBeInstanceOf(UnauthorizedException);
        expect(tokenRepository.replaceToken).not.toHaveBeenCalled();
    });

    it("refresh rotates refresh token and issues a new access token", async () => {
        const currentRefreshToken = "a".repeat(64);
        tokenRepository.findActiveByHash.mockResolvedValue({
            tokenId: 10,
            userId: existingUser.userId,
            tokenHash: createHash("sha256").update(currentRefreshToken).digest("hex"),
            expiresAt: new Date(Date.now() + 1000),
            revokedAt: null,
            createdAt: new Date(),
        });
        userRepository.findById.mockResolvedValue(existingUser);
        tokenRepository.replaceToken.mockResolvedValue({
            tokenId: 11,
            userId: existingUser.userId,
            tokenHash: "new-hash",
            expiresAt: new Date(Date.now() + 1000),
            revokedAt: null,
            createdAt: new Date(),
        });

        const result = await service.refresh(currentRefreshToken);

        expect(typeof result.accessToken).toBe("string");
        expect(result.refreshToken).toHaveLength(64);

        const decoded = jwt.verify(result.accessToken, "test-secret") as jwt.JwtPayload;
        expect(decoded.sub).toBe("42");
        expect(decoded.roles).toEqual(["CLIENT"]);

        expect(tokenRepository.replaceToken).toHaveBeenCalledWith(
            existingUser.userId,
            createHash("sha256").update(currentRefreshToken).digest("hex"),
            createHash("sha256").update(result.refreshToken).digest("hex"),
            expect.any(Date),
        );
    });

    it("logout revokes the provided refresh token", async () => {
        const refreshToken = "b".repeat(64);
        tokenRepository.findActiveByHash.mockResolvedValue({
            tokenId: 12,
            userId: existingUser.userId,
            tokenHash: createHash("sha256").update(refreshToken).digest("hex"),
            expiresAt: new Date(Date.now() + 1000),
            revokedAt: null,
            createdAt: new Date(),
        });
        userRepository.findById.mockResolvedValue(existingUser);
        tokenRepository.revokeByHash.mockResolvedValue(true);

        const result = await service.logout(refreshToken);

        expect(result).toEqual({ loggedOut: true });
        expect(tokenRepository.revokeByHash).toHaveBeenCalledWith(
            createHash("sha256").update(refreshToken).digest("hex"),
        );
    });

    it("throws when JWT_SECRET is missing", async () => {
        delete process.env.JWT_SECRET;
        userRepository.findByUsername.mockResolvedValue(existingUser);
        (bcrypt.compare as jest.Mock).mockResolvedValue(true);
        tokenRepository.createRefreshToken.mockResolvedValue({
            tokenId: 1,
            userId: existingUser.userId,
            tokenHash: "hash",
            expiresAt: new Date(Date.now() + 1000),
            revokedAt: null,
            createdAt: new Date(),
        } as RefreshTokenRecord);

        await expect(service.login("alice", "plain-pass")).rejects.toThrow("JWT_SECRET is required");
    });
});
