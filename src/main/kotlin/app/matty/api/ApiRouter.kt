package app.matty.api

import app.matty.api.auth.web.LoginHandler
import app.matty.api.auth.web.RefreshTokensHandler
import app.matty.api.interest.web.InterestsHandler
import app.matty.api.user.web.CurrentUserHandler
import app.matty.api.user.web.RegistrationHandler
import org.springframework.context.support.beans
import org.springframework.web.servlet.function.router

val apiRouter = beans {
    bean {
        router {
            "/login".nest {
                POST( ref<LoginHandler>()::login)
                GET("/code", ref<LoginHandler>()::getVerificationCode)
            }
            "/registration".nest {
                POST(ref<RegistrationHandler>()::register)
                GET("/code", ref<RegistrationHandler>()::getVerificationCode)
            }
            "/user".nest {
                GET("/me", ref<CurrentUserHandler>()::me)
            }
            GET("/auth/refresh", ref<RefreshTokensHandler>()::refreshAuthTokens)
            GET("/interests", ref<InterestsHandler>()::getAll)
        }
    }
}
