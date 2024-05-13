package packit.integration.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.jdbc.Sql
import packit.integration.IntegrationTest
import packit.integration.WithAuthenticatedUser
import packit.model.Role
import packit.model.RolePermission
import packit.model.User
import packit.model.dto.CreateRole
import packit.model.dto.UpdateRolePermission
import packit.model.dto.UpdateRolePermissions
import packit.model.dto.UpdateRoleUsers
import packit.model.toDto
import packit.repository.PermissionRepository
import packit.repository.RoleRepository
import packit.repository.UserRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Sql("/delete-test-users.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RoleControllerTest : IntegrationTest()
{
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var roleRepository: RoleRepository

    @Autowired
    private lateinit var permissionsRepository: PermissionRepository

    private val testCreateRole = CreateRole(
        name = "testRole",
        permissionNames = listOf("packet.run", "packet.read")
    )
    private val createTestRoleBody = ObjectMapper().writeValueAsString(
        testCreateRole
    )
    private val updateRolePermissions = ObjectMapper().writeValueAsString(
        UpdateRolePermissions(
            addPermissions = listOf(
                UpdateRolePermission(
                    permission = "packet.read"
                )
            ),
            removePermissions = listOf(
                UpdateRolePermission(
                    permission = "packet.run"
                )
            )
        )
    )

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users with manage authority can create roles`()
    {
        val result =
            restTemplate.postForEntity(
                "/role",
                getTokenizedHttpEntity(data = createTestRoleBody),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.CREATED)
        assertEquals(testCreateRole.name, ObjectMapper().readTree(result.body).get("name").asText())
        assertEquals(
            testCreateRole.permissionNames.size,
            ObjectMapper().readTree(result.body).get("rolePermissions").size()
        )
        assertNotNull(roleRepository.findByName("testRole"))
    }

    @Test
    @WithAuthenticatedUser(authorities = ["none"])
    fun `user without user manage permission cannot create roles`()
    {
        val result =
            restTemplate.postForEntity(
                "/role",
                getTokenizedHttpEntity(data = createTestRoleBody),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `reject request if createRole body is invalid`()
    {
        val result =
            restTemplate.postForEntity(
                "/role",
                getTokenizedHttpEntity(data = "{}"),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.BAD_REQUEST)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users with manage authority can delete roles`()
    {
        roleRepository.save(Role(name = "testRole"))

        val result =
            restTemplate.exchange(
                "/role/testRole",
                HttpMethod.DELETE,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.NO_CONTENT)
        assertNull(roleRepository.findByName("testRole"))
    }

    @Test
    @WithAuthenticatedUser(authorities = ["none"])
    fun `user without user manage permission cannot delete roles`()
    {
        roleRepository.save(Role(name = "testRole"))

        val result =
            restTemplate.postForEntity(
                "/role/testRole",
                getTokenizedHttpEntity(data = createTestRoleBody),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users with manage authority can update role permissions`()
    {
        val roleName = "testRole"
        val baseRole = roleRepository.save(Role(name = roleName))
        val permission = permissionsRepository.findByName("packet.run")!!
        baseRole.rolePermissions = mutableListOf(RolePermission(baseRole, permission))
        roleRepository.save(baseRole)

        val result = restTemplate.exchange(
            "/role/testRole/permissions",
            HttpMethod.PUT,
            getTokenizedHttpEntity(data = updateRolePermissions),
            String::class.java
        )

        assertEquals(result.statusCode, HttpStatus.NO_CONTENT)
        val role = roleRepository.findByName("testRole")!!
        assertEquals(1, role.rolePermissions.size)
        assertEquals("packet.read", role.rolePermissions.first().permission.name)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["none"])
    fun `user without user manage permission cannot update role permissions`()
    {
        roleRepository.save(Role(name = "testRole"))

        val result = restTemplate.exchange(
            "/role/testRole/permissions",
            HttpMethod.PUT,
            getTokenizedHttpEntity(data = updateRolePermissions),
            String::class.java
        )

        assertEquals(result.statusCode, HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `user with manage authority can read role names`()
    {
        roleRepository.save(Role(name = "testRole"))

        val result =
            restTemplate.exchange(
                "/role/names",
                HttpMethod.GET,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertSuccess(result)

        assertEquals(ObjectMapper().writeValueAsString(listOf("ADMIN", "testRole")), result.body)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users can get roles with relationships`()
    {
        val roleDto = roleRepository.findByName("ADMIN")!!.toDto()
        val result =
            restTemplate.exchange(
                "/role",
                HttpMethod.GET,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertSuccess(result)

        assertEquals(ObjectMapper().writeValueAsString(listOf(roleDto)), result.body)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users can get username roles with relationships `()
    {
        roleRepository.save(Role("username", isUsername = true))
        val allUsernameRoles = roleRepository.findAllByIsUsername(true).map { it.toDto() }
        val result =
            restTemplate.exchange(
                "/role?isUsername=true",
                HttpMethod.GET,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertSuccess(result)

        assertEquals(ObjectMapper().writeValueAsString(allUsernameRoles), result.body)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users can get non username roles with relationships`()
    {
        roleRepository.save(Role("randomUser", isUsername = true)).toDto()
        val adminRole = roleRepository.findByName("ADMIN")!!.toDto()
        val result =
            restTemplate.exchange(
                "/role?isUsername=false",
                HttpMethod.GET,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertSuccess(result)

        assertEquals(ObjectMapper().writeValueAsString(listOf(adminRole)), result.body)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users can get specific with relationships`()
    {
        val roleDto = roleRepository.findByName("ADMIN")!!.toDto()
        val result =
            restTemplate.exchange(
                "/role/ADMIN",
                HttpMethod.GET,
                getTokenizedHttpEntity(),
                String::class.java
            )

        assertSuccess(result)

        assertEquals(ObjectMapper().writeValueAsString(roleDto), result.body)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["none"])
    fun `user without user manage permission cannot update role users`()
    {
        val result =
            restTemplate.exchange(
                "/role/ADMIN/users",
                HttpMethod.PUT,
                getTokenizedHttpEntity(data = "{}"),
                String::class.java
            )

        assertEquals(result.statusCode, HttpStatus.UNAUTHORIZED)
    }

    @Test
    @WithAuthenticatedUser(authorities = ["user.manage"])
    fun `users with manage authority can update role users`()
    {
        val testRole = roleRepository.save(Role(name = "TEST_ROLE"))
        val userToRemove = User(
            username = "test",
            disabled = false,
            userSource = "basic",
            displayName = "test user",
            roles = mutableListOf(testRole)
        )
        val userToAdd = User(
            username = "test2",
            disabled = false,
            userSource = "github",
            displayName = "test user",
        )
        userRepository.saveAll(listOf(userToRemove, userToAdd))
        val updateRoleUsers = ObjectMapper().writeValueAsString(
            UpdateRoleUsers(
                usernamesToAdd = listOf(userToAdd.username),
                usernamesToRemove = listOf(userToRemove.username)
            )
        )

        val result = restTemplate.exchange(
            "/role/${testRole.name}/users",
            HttpMethod.PUT,
            getTokenizedHttpEntity(data = updateRoleUsers),
            String::class.java
        )

        assertSuccess(result)
        assertEquals(1, ObjectMapper().readTree(result.body).get("users").size())
        assertEquals(
            userToAdd.username,
            ObjectMapper().readTree(result.body).get("users").first().get("username").asText()
        )
        assertEquals(1, roleRepository.findByName(testRole.name)!!.users.size)
    }
}
