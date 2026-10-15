import { Body, Controller, Post, HttpCode } from "@nestjs/common";
import { AuthService } from "./auth.service";
import { LoginDto } from "../dto/login.dto";
import { RegisterDto } from "../dto/register.dto";
import {RefreshDto} from "../dto/refresh.dto";

@Controller("auth")
export class AuthController {
    constructor(private readonly authService: AuthService) {}

    @Post("register")
    register(@Body() body: RegisterDto) {
        return this.authService.register(body.username, body.password);
    }

    @Post("login")
    @HttpCode(200)
    login(@Body() body: LoginDto) {
        return this.authService.login(body.username, body.password);
    }

    @Post("refresh")
    @HttpCode(200)
    refresh(@Body() body: RefreshDto) {
        return this.authService.refresh(body.refreshToken);
    }
}
