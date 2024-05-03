package packit.unit.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.authority.SimpleGrantedAuthority
import packit.exceptions.PackitException
import packit.model.Packet
import packit.model.Permission
import packit.model.Role
import packit.model.RolePermission
import packit.model.dto.CreateRole
import packit.repository.RoleRepository
import packit.service.BaseRoleService
import packit.service.PermissionService
import packit.service.RolePermissionService
import kotlin.test.assertTrue

class RoleServiceTest
{
    private lateinit var roleRepository: RoleRepository
    private lateinit var roleService: BaseRoleService
    private lateinit var permissionService: PermissionService
    private lateinit var rolePermissionService: RolePermissionService

    @BeforeEach
    fun setup()
    {
        roleRepository = mock()
        permissionService = mock()
        rolePermissionService = mock()
        roleService = BaseRoleService(roleRepository, permissionService, rolePermissionService)
    }

    @Test
    fun `getUsernameRole returns existing role`()
    {
        val role = Role(name = "username")
        whenever(roleRepository.findByName("username")).thenReturn(role)

        val result = roleService.getUsernameRole("username")

        assertEquals(role, result)
    }

    @Test
    fun `getUsernameRole creates new role with is_username flag if not exists`()
    {
        whenever(roleRepository.findByName("username")).thenReturn(null)
        whenever(roleRepository.save(any<Role>())).thenAnswer { it.getArgument(0) }

        val result = roleService.getUsernameRole("username")

        assertEquals("username", result.name)
        assertEquals(true, result.isUsername)
        verify(roleRepository).save(any<Role>())
    }

    @Test
    fun `getAdminRole() returns existing role`()
    {
        val role = Role(name = "ADMIN")
        whenever(roleRepository.findByName("ADMIN")).thenReturn(role)

        val result = roleService.getAdminRole()

        assertEquals(role, result)
    }

    @Test
    fun `getAdminRole() creates new role if not exists`()
    {
        whenever(roleRepository.findByName("ADMIN")).thenReturn(null)
        whenever(roleRepository.save(any<Role>())).thenAnswer { it.getArgument(0) }

        val result = roleService.getAdminRole()

        assertEquals("ADMIN", result.name)
        verify(roleRepository).save(any<Role>())
    }

    @Test
    fun `createRole creates role with matching permissions`()
    {
        val createRole = CreateRole(name = "newRole", permissions = listOf("p1", "p2"))
        val permissions =
            listOf(Permission(name = "p1", description = "d1"), Permission(name = "p2", description = "d2"))
        whenever(permissionService.checkMatchingPermissions(createRole.permissions)).thenReturn(permissions)
        whenever(roleRepository.existsByName(createRole.name)).thenReturn(false)

        roleService.createRole(createRole)

        verify(roleRepository).save(
            argThat {
                this.name == createRole.name
                this.rolePermissions.size == 2
            }
        )
    }

    @Test
    fun `saveRole throws exception if role already exists`()
    {
        whenever(roleRepository.existsByName("roleName")).thenReturn(true)

        assertThrows(PackitException::class.java) {
            roleService.saveRole("roleName", listOf())
        }
    }

    @Test
    fun `saveRole saves role with permissions when does not exist`()
    {
        val roleName = "roleName"
        val permissions =
            listOf(Permission(name = "p1", description = "d1"), Permission(name = "p2", description = "d2"))
        whenever(roleRepository.existsByName("roleName")).thenReturn(false)

        roleService.saveRole(roleName, permissions)

        verify(roleRepository).save(
            argThat {
                this.name == roleName
                this.rolePermissions.size == 2
            }
        )
    }

    @Test
    fun `checkMatchingRoles throws exception if roles do not match`()
    {
        val rolesToCheck = listOf("role1", "role2")
        val allRoles = listOf(Role(name = "role1"))
        whenever(roleRepository.findByNameIn(rolesToCheck)).thenReturn(allRoles)

        assertThrows(PackitException::class.java) {
            roleService.checkMatchingRoles(rolesToCheck)
        }
    }

    @Test
    fun `checkMatchingRoles returns matching roles`()
    {
        val rolesToCheck = listOf("role1", "role2")
        val allRoles = listOf(Role(name = "role1"), Role(name = "role2"))
        whenever(roleRepository.findByNameIn(rolesToCheck)).thenReturn(allRoles)

        val result = roleService.checkMatchingRoles(rolesToCheck)

        assertEquals(2, result.size)
        assertEquals(allRoles, result)
    }

    @Test
    fun `getGrantedAuthorities returns authorities for roles and permissions`()
    {
        val role1 =
            createRoleWithPermission("role1", "permission1")
        val role2 =
            createRoleWithPermission("role2", "permission2")

        val result = roleService.getGrantedAuthorities(listOf(role1, role2))

        assertEquals(4, result.size)
        assertTrue(
            result.containsAll(
                listOf(
                    SimpleGrantedAuthority("role1"),
                    SimpleGrantedAuthority("permission1"),
                    SimpleGrantedAuthority("role2"),
                    SimpleGrantedAuthority("permission2")
                )
            )
        )
    }

    @Test
    fun `getPermissionScoped returns permission name with packet id`()
    {
        val rolePermission = createRoleWithPermission("role", "permission", "1").rolePermissions[0]

        val result = roleService.getPermissionScoped(rolePermission)

        assertEquals("permission:packet:1", result)
    }

    @Test
    fun `getPermissionScoped returns permission name with packetGroup id`()
    {
        val rolePermission = createRoleWithPermission("role", "permission", packetGroupId = 2).rolePermissions[0]

        val result = roleService.getPermissionScoped(rolePermission)

        assertEquals("permission:packetGroup:2", result)
    }

    @Test
    fun `getPermissionScoped returns permission name with tag id`()
    {
        val rolePermission = createRoleWithPermission("role", "permission", tagId = 3).rolePermissions[0]

        val result = roleService.getPermissionScoped(rolePermission)

        assertEquals("permission:tag:3", result)
    }

    @Test
    fun `getPermissionScoped returns permission name when no scope`()
    {
        val rolePermission = createRoleWithPermission("role", "permission").rolePermissions[0]

        val result = roleService.getPermissionScoped(rolePermission)

        assertEquals("permission", result)
    }

    @Test
    fun `deleteRole deletes existing role`()
    {
        val roleName = "existingRole"
        whenever(roleRepository.existsByName(roleName)).thenReturn(true)

        roleService.deleteRole(roleName)

        verify(roleRepository).deleteByName(roleName)
    }

    @Test
    fun `deleteRole throws exception if role does not exist`()
    {
        val roleName = "nonExistingRole"
        whenever(roleRepository.existsByName(roleName)).thenReturn(false)

        assertThrows<PackitException> {
            roleService.deleteRole(roleName)
        }
    }

    @Test
    fun `addPermissionsToRole throws exception when role does not exist`()
    {
        val roleName = "nonExistingRole"
        whenever(roleRepository.findByName(roleName)).thenReturn(null)

        assertThrows<PackitException> {
            roleService.addPermissionsToRole(roleName, listOf())
        }.apply {
            assertEquals("roleNotFound", key)
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus)
        }
    }

    @Test
    fun `addPermissionsToRole throws exception when role permission already exists`()
    {
        val roleName = "roleName"
        val permissionName = "permission1"
        val role = createRoleWithPermission(roleName, permissionName)
        whenever(roleRepository.findByName(roleName)).thenReturn(role)
        whenever(rolePermissionService.getRolePermissionsToUpdate(role, listOf())).thenReturn(
            listOf(
                createRoleWithPermission(roleName, permissionName).rolePermissions.first()
            )
        )

        assertThrows<PackitException> {
            roleService.addPermissionsToRole(roleName, listOf())
        }.apply {
            assertEquals("rolePermissionAlreadyExists", key)
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus)
        }
    }

    @Test
    fun `addPermissionsToRole calls getRolePermissionsToUpdate and saves role with added role permission`()
    {
        val roleName = "roleName"
        val permissionName = "permission1"
        val role = createRoleWithPermission(roleName, permissionName)
        whenever(roleRepository.findByName(roleName)).thenReturn(role)
        whenever(rolePermissionService.getRolePermissionsToUpdate(role, listOf())).thenReturn(
            listOf(
                createRoleWithPermission(roleName, "differentPermission").rolePermissions.first()
            )
        )

        roleService.addPermissionsToRole(roleName, listOf())

        verify(roleRepository).save(
            argThat {
                this == role
                this.rolePermissions.size == 2
            }
        )
    }

    @Test
    fun `removePermissionsFromRole throws exception when role does not exist`()
    {
        val roleName = "nonExistingRole"
        whenever(roleRepository.findByName(roleName)).thenReturn(null)

        assertThrows<PackitException> {
            roleService.removePermissionsFromRole(roleName, listOf())
        }.apply {
            assertEquals("roleNotFound", key)
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus)
        }
    }

    @Test
    fun `removePermissionsFromRole calls removeRolePermissionsFromRole`()
    {
        val roleName = "roleName"
        val role = Role(name = roleName)
        whenever(roleRepository.findByName(roleName)).thenReturn(role)

        roleService.removePermissionsFromRole(roleName, listOf())

        verify(rolePermissionService).removeRolePermissionsFromRole(role, listOf())
    }

    @Test
    fun `getRoleNames returns role names`()
    {
        val roles = listOf(Role(name = "role1"), Role(name = "role2"))
        whenever(roleRepository.findAll()).thenReturn(roles)

        val result = roleService.getRoleNames()

        assertEquals(2, result.size)
        assertTrue(result.containsAll(listOf("role1", "role2")))
    }

    @Test
    fun `getRolesWithRelationships returns all roles`()
    {
        val roles = listOf(Role(name = "role1"), Role(name = "role2"))
        whenever(roleRepository.findAll()).thenReturn(roles)

        val result = roleService.getRolesWithRelationships()

        assertEquals(2, result.size)
        assertTrue(result.containsAll(roles))
    }

    @Test
    fun `getRolesWithRelationships returns matching roles when all role names exist`()
    {
        val roleNames = listOf("role1", "role2")
        val roles = listOf(Role(name = "role1"), Role(name = "role2"))
        whenever(roleRepository.findByNameIn(roleNames)).thenReturn(roles)

        val result = roleService.getRolesWithRelationships(roleNames)

        assertEquals(roles, result)
    }

    @Test
    fun `getRolesWithRelationships throws exception when some role names do not exist`()
    {
        val roleNames = listOf("role1", "role2")
        val roles = listOf(Role(name = "role1"))
        whenever(roleRepository.findByNameIn(roleNames)).thenReturn(roles)

        assertThrows<PackitException> {
            roleService.getRolesWithRelationships(roleNames)
        }.apply {
            assertEquals("invalidRolesProvided", key)
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus)
        }
    }

    @Test
    fun `getRolesWithRelationships throws exception when no role names exist`()
    {
        val roleNames = listOf("role1", "role2")
        whenever(roleRepository.findByNameIn(roleNames)).thenReturn(emptyList())

        assertThrows<PackitException> {
            roleService.getRolesWithRelationships(roleNames)
        }.apply {
            assertEquals("invalidRolesProvided", key)
            assertEquals(HttpStatus.BAD_REQUEST, httpStatus)
        }
    }

    @Test
    fun `getRole returns role by name`()
    {
        val roleName = "roleName"
        val role = Role(name = roleName)
        whenever(roleRepository.findByName(roleName)).thenReturn(role)

        val result = roleService.getRole(roleName)

        assertEquals(role, result)
    }

    private fun createRoleWithPermission(
        roleName: String,
        permissionName: String,
        packetId: String? = null,
        packetGroupId: Int? = null,
        tagId: Int? = null
    ): Role
    {
        return Role(
            name = roleName,
            rolePermissions = mutableListOf(
                RolePermission(
                    permission = Permission(
                        name = permissionName,
                        description = "description does not matter"
                    ),
                    role = Role(name = roleName),
                    packet = packetId?.let { mock<Packet> { on { id } doReturn packetId } },
                    packetGroup = packetGroupId?.let { mock { on { id } doReturn packetGroupId } },
                    tag = tagId?.let { mock { on { id } doReturn tagId } }
                )
            )
        )
    }
}
