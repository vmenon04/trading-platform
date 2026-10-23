import { Module } from "@nestjs/common";
import { ConfigModule } from "@nestjs/config";
import { AuthController } from "./auth/auth.controller";
import { AuthService } from "./auth/auth.service";
import {UserRepository} from "./auth/user.repository";
import {TokenRepository} from "./auth/token.repository";

@Module({
    imports: [
        ConfigModule.forRoot({
            isGlobal: true,
            envFilePath: ".env",
        })
    ],
    controllers: [AuthController],
    providers: [AuthService, UserRepository, TokenRepository],
})
export class AppModule {}
