import { Injectable, OnModuleDestroy } from "@nestjs/common";
import { Pool, PoolClient } from "pg";

export interface RefreshTokenRecord {
	tokenId: number;
	userId: number;
	tokenHash: string;
	expiresAt: Date;
	revokedAt: Date | null;
	createdAt: Date;
}

interface RefreshTokenRow {
	token_id: string;
	user_id: string;
	token_hash: string;
	expires_at: Date | string;
	revoked_at: Date | string | null;
	created_at: Date | string;
}

@Injectable()
export class TokenRepository implements OnModuleDestroy {
	private readonly pool: Pool;

	constructor() {
		this.pool = new Pool({ connectionString: process.env.DATABASE_URL });
	}

	async onModuleDestroy(): Promise<void> {
		await this.pool.end();
	}

	async createRefreshToken(userId: number, tokenHash: string, expiresAt: Date): Promise<RefreshTokenRecord> {
		const row = await this.queryOne<RefreshTokenRow>(
			`INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
			 VALUES ($1, $2, $3)
			 RETURNING token_id, user_id, token_hash, expires_at, revoked_at, created_at`,
			[userId, tokenHash, expiresAt],
		);

		if (!row) {
			throw new Error("failed to insert refresh token");
		}

		return this.mapRow(row);
	}

	async findActiveByHash(tokenHash: string): Promise<RefreshTokenRecord | null> {
		const row = await this.queryOne<RefreshTokenRow>(
			`SELECT token_id, user_id, token_hash, expires_at, revoked_at, created_at
			 FROM refresh_tokens
			 WHERE token_hash = $1
			   AND revoked_at IS NULL
			   AND expires_at > CURRENT_TIMESTAMP`,
			[tokenHash],
		);

		return row ? this.mapRow(row) : null;
	}

	async findById(tokenId: number): Promise<RefreshTokenRecord | null> {
		const row = await this.queryOne<RefreshTokenRow>(
			`SELECT token_id, user_id, token_hash, expires_at, revoked_at, created_at
			 FROM refresh_tokens
			 WHERE token_id = $1`,
			[tokenId],
		);

		return row ? this.mapRow(row) : null;
	}

	async revokeById(tokenId: number): Promise<boolean> {
		const result = await this.pool.query(
			`UPDATE refresh_tokens
			 SET revoked_at = CURRENT_TIMESTAMP
			 WHERE token_id = $1
			   AND revoked_at IS NULL`,
			[tokenId],
		);

		return (result.rowCount ?? 0) > 0;
	}

	async revokeByHash(tokenHash: string): Promise<boolean> {
		const result = await this.pool.query(
			`UPDATE refresh_tokens
			 SET revoked_at = CURRENT_TIMESTAMP
			 WHERE token_hash = $1
			   AND revoked_at IS NULL`,
			[tokenHash],
		);

		return (result.rowCount ?? 0) > 0;
	}

	async revokeAllForUser(userId: number): Promise<number> {
		const result = await this.pool.query(
			`UPDATE refresh_tokens
			 SET revoked_at = CURRENT_TIMESTAMP
			 WHERE user_id = $1
			   AND revoked_at IS NULL`,
			[userId],
		);

		return result.rowCount ?? 0;
	}

	async replaceToken(
		userId: number,
		oldTokenHash: string,
		newTokenHash: string,
		expiresAt: Date,
	): Promise<RefreshTokenRecord> {
		return this.withTransaction(async (client) => {
			const revokeResult = await client.query(
				`UPDATE refresh_tokens
				 SET revoked_at = CURRENT_TIMESTAMP
				 WHERE user_id = $1
				   AND token_hash = $2
				   AND revoked_at IS NULL`,
				[userId, oldTokenHash],
			);

			if ((revokeResult.rowCount ?? 0) === 0) {
				throw new Error("failed to revoke old refresh token");
			}

			const inserted = await client.query<RefreshTokenRow>(
				`INSERT INTO refresh_tokens (user_id, token_hash, expires_at)
				 VALUES ($1, $2, $3)
				 RETURNING token_id, user_id, token_hash, expires_at, revoked_at, created_at`,
				[userId, newTokenHash, expiresAt],
			);

			const row = inserted.rows[0];
			if (!row) {
				throw new Error("failed to insert refresh token");
			}

			return this.mapRow(row);
		});
	}

	private async queryOne<T extends object>(sql: string, params: unknown[]): Promise<T | null> {
		const result = await this.pool.query<T>(sql, params);
		return result.rows[0] ?? null;
	}

	private async withTransaction<T>(work: (client: PoolClient) => Promise<T>): Promise<T> {
		const client = await this.pool.connect();
		try {
			await client.query("BEGIN");
			const value = await work(client);
			await client.query("COMMIT");
			return value;
		} catch (error) {
			await client.query("ROLLBACK");
			throw error;
		} finally {
			client.release();
		}
	}

	private mapRow(row: RefreshTokenRow): RefreshTokenRecord {
		return {
			tokenId: Number(row.token_id),
			userId: Number(row.user_id),
			tokenHash: row.token_hash,
			expiresAt: row.expires_at instanceof Date ? row.expires_at : new Date(row.expires_at),
			revokedAt: row.revoked_at === null ? null : row.revoked_at instanceof Date ? row.revoked_at : new Date(row.revoked_at),
			createdAt: row.created_at instanceof Date ? row.created_at : new Date(row.created_at),
		};
	}
}


