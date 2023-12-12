package packit.unit.clients

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.kohsuke.github.*
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import packit.AppConfig
import packit.clients.GithubUserClient
import packit.exceptions.PackitAuthenticationException
import kotlin.test.Test
import kotlin.test.assertEquals

class GithubUserClientTest {

    private val mockConfig = mock<AppConfig> {
        on { authGithubAPIOrgs } doReturn "vimc,mrc-ide"
    }

    private val mockOrg = mock<GHOrganization>{
        on { login } doReturn "mrc-ide"
    }
    private val mockOrgs = mock<GHPersonSet<GHOrganization>> {
        on { iterator() } doReturn mutableListOf(mockOrg).listIterator()
    }

    private val mockMyself = mock<GHMyself> {
        on { login } doReturn "test@login.com"
        on { name } doReturn "test name"
        on { allOrganizations } doReturn mockOrgs
    }

    private val mockGitHub = mock<GitHub> {
        on { myself } doReturn mockMyself
    }

    private val mockGithubBuilder = mock<GitHubBuilder> { thisMock ->
        on { withOAuthToken(anyString()) } doReturn thisMock
        on { build() } doReturn mockGitHub
    }
    private val sut = GithubUserClient(mockConfig, mockGithubBuilder)

    private val token = "12345"

    @Test
    fun `can authenticate`()
    {
        sut.authenticate(token)
        verify(mockGithubBuilder).withOAuthToken(token)
        verify(mockGithubBuilder).build()
    }

    @Test
    fun `can getUser`()
    {
        sut.authenticate(token)
        val user = sut.getUser()
        assertEquals(user.displayName, "test name")
        assertEquals(user.name, "test@login.com")
    }

    @Test
    fun `can check github org membership`()
    {
        sut.authenticate(token)
        sut.checkGithubOrgMembership()
    }

    @Test
    fun `throws expected exception when user is not in allowed org`()
    {
        val mockErrorConfig = mock<AppConfig> {
            on { authGithubAPIOrgs } doReturn "vimc,mrc-idex"
        }
        val errorSut = GithubUserClient(mockErrorConfig, mockGithubBuilder)
        errorSut.authenticate(token)
        assertThatThrownBy { errorSut.checkGithubOrgMembership() }
            .isInstanceOf(PackitAuthenticationException::class.java)
            .matches { (it as PackitAuthenticationException).key === "githubUserRestrictedAccess" }
            .matches { (it as PackitAuthenticationException).httpStatus === HttpStatus.UNAUTHORIZED }
    }

    private fun assertHandlesHttpExceptionOnAuthenticate(
        exceptionStatusCode: Int,
        expectedPackitExceptionKey: String,
        expectedPackitExceptionStatus: HttpStatus
    )
    {
        val mockErroringGithub = mock<GitHub> {
            on { myself } doThrow HttpException("test error", exceptionStatusCode, "", "")
        }
        val mockErroringGhBuilder = mock<GitHubBuilder> { thisMock ->
            on { withOAuthToken(anyString()) } doReturn thisMock
            on { build() } doReturn mockErroringGithub
        }

        val errorSut = GithubUserClient(mockConfig, mockErroringGhBuilder)
        assertThatThrownBy { errorSut.authenticate("token") }
            .isInstanceOf(PackitAuthenticationException::class.java)
            .matches { (it as PackitAuthenticationException).key === expectedPackitExceptionKey }
            .matches { (it as PackitAuthenticationException).httpStatus === expectedPackitExceptionStatus }
    }

    @Test
    fun `handles unauthorized error on authenticate`()
    {
        assertHandlesHttpExceptionOnAuthenticate(401, "githubTokenInsufficientPermissions", HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `handles other Http exceptions on authenticate`()
    {
        assertHandlesHttpExceptionOnAuthenticate(400, "githubTokenUnexpectedError", HttpStatus.BAD_REQUEST)
    }

    private fun assertThrowsUserNotAutheticated(call: () -> Any)
    {
        assertThatThrownBy { call() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessage("User has not been authenticated")
    }

    @Test
    fun `handles not authenticated on getUser`()
    {
        assertThrowsUserNotAutheticated { sut.getUser() }
    }

    @Test
    fun `handles not authenticated on checkGithubOrgMembership`()
    {
        assertThrowsUserNotAutheticated { sut.checkGithubOrgMembership() }
    }
}
