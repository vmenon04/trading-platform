import {Injectable, UnauthorizedException, ConflictException, Inject} from "@nestjs/common";
import * as bcrypt from "bcrypt";
import * as jwt from "jsonwebtoken";
import { randomBytes } from "crypto";
import { logAuthEvent } from "../logger";
import {UserRecord, UserRepository, UserRole} from "./user.repository";
import {TokenRepository} from "./token.repository";
import {createHash} from "node:crypto";

const SALT_ROUNDS = 10;


@Injectable()
export class AuthService {

    constructor(
        @Inject(UserRepository) private readonly userRepository: UserRepository,
        @Inject(TokenRepository) private readonly tokenRepository: TokenRepository
    ) {}

    async register(
        username: string,
        password: string,
        roles: UserRole[] = ["CLIENT"],
    ): Promise<{ username: string; registered: true }> {
        if (await this.userRepository.existsByUsername(username)) {
            throw new ConflictException(`${username} is already registered`);
        }
        const passwordHash = await bcrypt.hash(password, SALT_ROUNDS);
        await this.userRepository.createUser({username, passwordHash, roles: roles});
        return { username, registered: true };
    }

    async login(username: string, password: string): Promise<{ accessToken: string; refreshToken: string }> {
        const user = await this.userRepository.findByUsername(username);
        const passwordMatches = user ? await bcrypt.compare(password, user.passwordHash) : false;
        if (!user || !passwordMatches) {
            throw new UnauthorizedException("invalid username or password");
        }
        const accessToken = this.issueAccessToken(user.username, user.roles);
        const refreshToken = randomBytes(32).toString("hex");
        const hashedRefreshToken = this.hashRefreshToken(refreshToken);
        await this.tokenRepository.createRefreshToken(user.userId, hashedRefreshToken, new Date(Date.now() + 6 * 7 * 24 * 60 * 60 * 1000));
        logAuthEvent("login_success", user.username);
        return { accessToken, refreshToken };
    }

    async refresh(refreshToken: string): Promise<{ refreshToken: string, accessToken: string }> {
        const entry = await this.findByRefreshToken(refreshToken);
        if (!entry) {
            throw new UnauthorizedException("invalid or expired refresh token");
        }
        const user = entry;
        const newRefreshToken = randomBytes(32).toString("hex");
        const newHashedRefreshToken = this.hashRefreshToken(newRefreshToken);
        await this.tokenRepository.replaceToken(user.userId, this.hashRefreshToken(refreshToken), newHashedRefreshToken, new Date(Date.now() + 6 * 7 * 24 * 60 * 60 * 1000));
        return {
            refreshToken: newRefreshToken,
            accessToken: this.issueAccessToken(user.username, user.roles)
        };
    }

    private issueAccessToken(username: string, roles: string[]): string {
        return jwt.sign({ sub: username, roles }, this.getJwtSecret(), {
            algorithm: "HS256",
            expiresIn: "15m",
        });
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
