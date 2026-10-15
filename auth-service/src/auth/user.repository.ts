import { Injectable, OnModuleDestroy } from "@nestjs/common";
import { randomUUID } from "crypto";
import { Pool, PoolClient } from "pg";

export type UserRole = "ADMIN" | "CLIENT" | "ANALYST";

export interface UserRecord {
	userId: number;
	externalUserId: string;
	username: string;
	passwordHash: string;
	clientId: number | null;
	roles: UserRole[];
	createdAt: Date;
}

export interface CreateUserInput {
	username: string;
	passwordHash: string;
	clientId?: number | null;
	externalUserId?: string;
	roles?: UserRole[];
}

interface UserRow {
	user_id: string;
	external_user_id: string;
	username: string;
	password_hash: string;
	client_id: string | null;
	created_at: Date | string | null;
}

interface RoleRow {
	role: UserRole;
}

const ALLOWED_ROLES: readonly UserRole[] = ["ADMIN", "CLIENT", "ANALYST"];

@Injectable()
export class UserRepository implements OnModuleDestroy {
	private readonly pool: Pool;

	constructor() {
		this.pool = new Pool({ connectionString: process.env.DATABASE_URL });
	}

	async onModuleDestroy(): Promise<void> {
		await this.pool.end();
	}

	async findById(userId: number): Promise<UserRecord | null> {
		const user = await this.queryOne<UserRow>(
			`SELECT user_id, external_user_id, username, password_hash, client_id, created_at
			 FROM users
			 WHERE user_id = $1`,
			[userId],
		);

		if (!user) {
			return null;
		}

		return this.mapUser(user, await this.findRolesByUserId(userId));
	}

	async findByUsername(username: string): Promise<UserRecord | null> {
		const user = await this.queryOne<UserRow>(
			`SELECT user_id, external_user_id, username, password_hash, client_id, created_at
			 FROM users
			 WHERE username = $1`,
			[username],
		);

		if (!user) {
			return null;
		}

		return this.mapUser(user, await this.findRolesByUserId(Number(user.user_id)));
	}

	async existsByUsername(username: string): Promise<boolean> {
		const result = await this.pool.query<{ exists: boolean }>(
			`SELECT EXISTS(
				SELECT 1
				FROM users
				WHERE username = $1
			) AS exists`,
			[username],
		);

		return result.rows[0]?.exists ?? false;
	}

	async createUser(input: CreateUserInput): Promise<UserRecord> {
		const userExternalId = input.externalUserId ?? randomUUID();
		const roles = this.normalizeRoles(input.roles ?? []);

		return this.withTransaction(async (client) => {
			const inserted = await client.query<UserRow>(
				`INSERT INTO users (external_user_id, username, password_hash, client_id)
				 VALUES ($1, $2, $3, $4)
				 RETURNING user_id, external_user_id, username, password_hash, client_id, created_at`,
				[userExternalId, input.username, input.passwordHash, input.clientId ?? null],
			);

			const user = inserted.rows[0];
			if (!user) {
				throw new Error("failed to insert user");
			}

			if (roles.length > 0) {
				await this.insertRoles(client, Number(user.user_id), roles);
			}

			return this.mapUser(user, roles);
		});
	}

	async updatePasswordHash(userId: number, passwordHash: string): Promise<void> {
		await this.pool.query(
			`UPDATE users
			 SET password_hash = $2
			 WHERE user_id = $1`,
			[userId, passwordHash],
		);
	}

	async updateClientId(userId: number, clientId: number | null): Promise<void> {
		await this.pool.query(
			`UPDATE users
			 SET client_id = $2
			 WHERE user_id = $1`,
			[userId, clientId],
		);
	}

	async replaceRoles(userId: number, roles: UserRole[]): Promise<UserRecord | null> {
		const normalized = this.normalizeRoles(roles);

		await this.withTransaction(async (client) => {
			await client.query(`DELETE FROM roles WHERE user_id = $1`, [userId]);
			if (normalized.length > 0) {
				await this.insertRoles(client, userId, normalized);
			}
		});

		return this.findById(userId);
	}

	async addRole(userId: number, role: UserRole): Promise<void> {
		this.assertValidRole(role);
		await this.pool.query(
			`INSERT INTO roles (user_id, role)
			 VALUES ($1, $2)
			 ON CONFLICT DO NOTHING`,
			[userId, role],
		);
	}

	async removeRole(userId: number, role: UserRole): Promise<void> {
		await this.pool.query(
			`DELETE FROM roles
			 WHERE user_id = $1 AND role = $2`,
			[userId, role],
		);
	}

	private async findRolesByUserId(userId: number): Promise<UserRole[]> {
		const result = await this.pool.query<RoleRow>(
			`SELECT role
			 FROM roles
			 WHERE user_id = $1
			 ORDER BY role`,
			[userId],
		);

		return result.rows.map((row) => row.role);
	}

	private async insertRoles(client: PoolClient, userId: number, roles: UserRole[]): Promise<void> {
		await client.query(
			`INSERT INTO roles (user_id, role)
			 SELECT $1, UNNEST($2::text[])`,
			[userId, roles],
		);
	}

	private mapUser(user: UserRow, roles: UserRole[]): UserRecord {
		return {
			userId: Number(user.user_id),
			externalUserId: user.external_user_id,
			username: user.username,
			passwordHash: user.password_hash,
			clientId: user.client_id === null ? null : Number(user.client_id),
			roles,
			createdAt: user.created_at instanceof Date ? user.created_at : new Date(user.created_at ?? Date.now()),
		};
	}

	private normalizeRoles(roles: UserRole[]): UserRole[] {
		const uniqueRoles = [...new Set(roles)];
		uniqueRoles.forEach((role) => this.assertValidRole(role));
		return uniqueRoles;
	}

	private assertValidRole(role: string): asserts role is UserRole {
		if (!ALLOWED_ROLES.includes(role as UserRole)) {
			throw new Error(`invalid role "${role}"; expected one of ${ALLOWED_ROLES.join(", ")}`);
		}
	}

	private async queryOne<T extends object>(sql: string, params: unknown[]): Promise<T | null> {
		const result = await this.pool.query<T>(sql, params);
		return result.rows[0] ?? null;
	}

	private async withTransaction<T>(work: (client: PoolClient) => Promise<T>): Promise<T> {
		const client = await this.pool.connect();
		try {
			await client.query("BEGIN");
			const result = await work(client);
			await client.query("COMMIT");
			return result;
		} catch (error) {
			await client.query("ROLLBACK");
			throw error;
		} finally {
			client.release();
		}
	}

}
