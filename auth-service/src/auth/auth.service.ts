import {Injectable, UnauthorizedException, ConflictException, Inject} from "@nestjs/common";
import * as bcrypt from "bcrypt";
import * as jwt from "jsonwebtoken";
import { randomBytes } from "crypto";
import { logAuthEvent } from "../logger";
import {UserRecord, UserRepository, UserRole} from "./user.repository";
import {TokenRepository} from "./token.repository";
import {createHash} from "node:crypto";

const SALT_ROUNDS = 10;
const REFRESH_TOKEN_TTL_MS = 6 * 7 * 24 * 60 * 60 * 1000;

interface TokenPair {
    accessToken: string;
    refreshToken: string;
}


@Injectable()
export class AuthService {

    constructor(
        @Inject(UserRepository) private readonly userRepository: UserRepository,
        @Inject(TokenRepository) private readonly tokenRepository: TokenRepository
    ) {}

    async register(
        username: string,
        password: string,
        email: string,
        roles: UserRole[] = ["CLIENT"],
    ): Promise<{ username: string; registered: true }> {
        if (await this.userRepository.existsByUsername(username)) {
            throw new ConflictException(`${username} is already registered`);
        }
        if (await this.userRepository.existsByEmail(email)) {
            throw new ConflictException(`${email} is already registered`);
        }
        const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
        try {
            await this.userRepository.createUser({username, passwordHash, roles: roles});
        } catch (error) {
            // The username unique constraint covers a race after the pre-check.
            if (isUniqueViolation(error)) {
                throw new ConflictException(`${username} is already registered`);
            }
            throw error;
        }
        return { username, registered: true };
    }

    async login(username: string, password: string): Promise<TokenPair> {
        const user = await this.userRepository.findByUsername(username);
        const passwordMatches = user ? await bcrypt.compare(password, user.passwordHash) : false;
        if (!user || !passwordMatches) {
            throw new UnauthorizedException("invalid username or password");
        }
        logAuthEvent("login_success", user.username);
        return this.issueTokenPair(user);
    }

    async refresh(refreshToken: string): Promise<TokenPair> {
        const user = await this.findByRefreshToken(refreshToken);
        if (!user) {
            throw new UnauthorizedException("user not currently logged in");
        }
        return this.rotateTokenPair(user, refreshToken);
    }

    async logout(refreshToken: string): Promise<{ loggedOut: true }> {
        const user = await this.findByRefreshToken(refreshToken);
        if (!user) {
            throw new UnauthorizedException("user not currently logged in");
        }
        await this.tokenRepository.revokeByHash(this.hashRefreshToken(refreshToken));
        return { loggedOut: true };
    }

    private issueAccessToken(userId: bigint, roles: string[]): string {
        return jwt.sign({ sub: userId.toString(), roles }, this.getJwtSecret(), {
            algorithm: "HS256",
            expiresIn: "15m",
        });
    }

    private async issueTokenPair(user: UserRecord): Promise<TokenPair> {
        const refreshToken = this.generateRefreshToken();
        await this.tokenRepository.createRefreshToken(
                user.userId,
                this.hashRefreshToken(refreshToken),
                this.getRefreshTokenExpiryDate(),
        );

        return {
            accessToken: this.issueAccessToken(user.userId, user.roles),
            refreshToken,
        };
    }

    private async rotateTokenPair(user: UserRecord, refreshToken: string): Promise<TokenPair> {
        const newRefreshToken = this.generateRefreshToken();
        await this.tokenRepository.replaceToken(
                user.userId,
                this.hashRefreshToken(refreshToken),
                this.hashRefreshToken(newRefreshToken),
                this.getRefreshTokenExpiryDate(),
        );

        return {
            accessToken: this.issueAccessToken(user.userId, user.roles),
            refreshToken: newRefreshToken,
        };
    }

    private generateRefreshToken(): string {
        return randomBytes(32).toString("hex");
    }

    private getRefreshTokenExpiryDate(): Date {
        return new Date(Date.now() + REFRESH_TOKEN_TTL_MS);
    }

    private hashRefreshToken(token: string): string {
        return createHash("sha256").update(token).digest("hex");
    }

    private async findByRefreshToken(refreshToken: string): Promise<UserRecord | null> {
        const hashedToken = this.hashRefreshToken(refreshToken);
        const token = await this.tokenRepository.findActiveByHash(hashedToken);
        if (!token) {
            return null;
        }
        const user = await this.userRepository.findById(token.userId);
        if (!user) {
            return null;
        }
        return user;
    }

    private getJwtSecret(): string {
        const secret = process.env.JWT_SECRET;
        if (!secret) {
            throw new Error("JWT_SECRET is required");
        }
        return secret;
    }
}

function isUniqueViolation(error: unknown): boolean {
    return typeof error === "object" && error !== null && "code" in error && error.code === "23505";
}
