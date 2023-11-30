package packit.clients

import org.kohsuke.github.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import packit.AppConfig
import packit.exceptions.PackitAuthenticationException
import packit.exceptions.PackitException
import packit.model.User
import packit.security.Role
import packit.security.profile.UserPrincipal

@Component
class GithubUserClient(private val config: AppConfig, private val githubBuilder: GitHubBuilder = GitHubBuilder()) {

    private var github: GitHub? = null
    private var ghUser: GHMyself? = null

    fun authenticate(token: String)
    {
        connectToClient(token)
        ghUser = getGitHubUser()
    }

    fun getUser(): UserPrincipal
    {
        checkAuthenticated()
        val ghu = ghUser!!

        val user = User(
            1L,
            ghu.login,
            "",
            Role.USER,
            ghu.name ?: "",
            mutableMapOf()
        )
        return UserPrincipal.create(user, mutableMapOf())
    }

    fun checkGithubMembership()
    {
        checkAuthenticated()

        val userOrg = ghUser!!.allOrganizations.firstOrNull { org -> org.login == config.authGithubAPIOrg }
        var userAllowed = userOrg != null

        val allowedTeam = config.authGithubAPITeam
        if (userAllowed && !allowedTeam.isEmpty())
        {
            // We've confirmed user is in org, and required team is not empty, so we need to check that too
            val team = userOrg!!.teams[allowedTeam] ?: throw PackitAuthenticationException("githubConfigTeamNotInOrg",
                HttpStatus.UNAUTHORIZED)
            userAllowed = ghUser!!.isMemberOf(team)
        }

        if (!userAllowed)
        {
            throw PackitAuthenticationException("githubUserRestrictedAccess", HttpStatus.UNAUTHORIZED)
        }
    }

    private fun checkAuthenticated()
    {
        checkNotNull(ghUser) { "User has not been authenticated" }
    }

    private fun connectToClient(token: String)
    {
       github = githubBuilder.withOAuthToken(token).build()
    }

    private fun getGitHubUser(): GHMyself
    {
        try
        {
            return github!!.myself
        }
        catch(e: HttpException)
        {
            throw throwOnHttpException(e)
        }
    }

    private fun throwOnHttpException(e: HttpException): Exception
    {
        val errorCode = if (e.responseCode == HttpStatus.UNAUTHORIZED.value()) {
            "githubTokenInsufficientPermissions"
        }
        else {
            "githubTokenUnexpectedError"
        }
        return PackitAuthenticationException(errorCode, HttpStatus.valueOf(e.responseCode))
    }
}
