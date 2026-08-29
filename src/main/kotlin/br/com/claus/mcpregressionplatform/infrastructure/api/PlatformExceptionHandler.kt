package br.com.claus.mcpregressionplatform.infrastructure.api

import br.com.claus.mcpregressionplatform.application.contract.UnknownContractException
import br.com.claus.mcpregressionplatform.application.dependency.UnknownBffException
import br.com.claus.mcpregressionplatform.application.dependency.UnknownDependencyException
import br.com.claus.mcpregressionplatform.domain.security.AuthorizationDeniedException
import br.com.claus.mcpregressionplatform.infrastructure.mcp.security.InvalidToolInputException
import br.com.claus.mcpregressionplatform.infrastructure.security.InvalidCredentialsException
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class PlatformExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException::class)
    fun invalidCredentials(exception: InvalidCredentialsException): ProblemDetail =
        problem(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.message)

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun authorizationDenied(exception: AuthorizationDeniedException): ProblemDetail =
        problem(HttpStatus.FORBIDDEN, "Authorization denied", exception.message)

    @ExceptionHandler(InvalidToolInputException::class, IllegalArgumentException::class)
    fun invalidInput(exception: RuntimeException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Invalid input", exception.message)

    @ExceptionHandler(
        UnknownBffException::class,
        UnknownDependencyException::class,
        UnknownContractException::class
    )
    fun notFound(exception: RuntimeException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Resource not found", exception.message)

    private fun problem(status: HttpStatus, title: String, detail: String?): ProblemDetail {
        val problem = ProblemDetail.forStatus(status)
        problem.title = title
        problem.detail = detail
        return problem
    }
}
