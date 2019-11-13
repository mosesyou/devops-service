package io.choerodon.devops.api.controller.v1

import static org.mockito.ArgumentMatchers.*
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT

import com.github.pagehelper.PageInfo
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Subject

import io.choerodon.devops.IntegrationTestConfiguration
import io.choerodon.devops.api.vo.DevopsIngressPathVO
import io.choerodon.devops.api.vo.DevopsIngressVO
import io.choerodon.devops.api.vo.iam.ProjectWithRoleVO
import io.choerodon.devops.api.vo.iam.RoleVO
import io.choerodon.devops.app.service.DevopsEnvCommandService
import io.choerodon.devops.app.service.DevopsEnvironmentService
import io.choerodon.devops.app.service.DevopsProjectService
import io.choerodon.devops.app.service.GitlabGroupMemberService
import io.choerodon.devops.infra.dto.*
import io.choerodon.devops.infra.dto.gitlab.MemberDTO
import io.choerodon.devops.infra.dto.iam.OrganizationDTO
import io.choerodon.devops.infra.dto.iam.ProjectDTO
import io.choerodon.devops.infra.enums.AccessLevel
import io.choerodon.devops.infra.enums.CertificationStatus
import io.choerodon.devops.infra.feign.operator.BaseServiceClientOperator
import io.choerodon.devops.infra.feign.operator.GitlabServiceClientOperator
import io.choerodon.devops.infra.handler.ClusterConnectionHandler
import io.choerodon.devops.infra.mapper.*
import io.choerodon.devops.infra.util.FileUtil
import io.choerodon.devops.infra.util.GitUtil

/**
 * Created by n!Ck
 * Date: 2018/9/12
 * Time: 11:07
 * Description: 
 */

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(IntegrationTestConfiguration)
@Subject(DevopsIngressController)
@Stepwise
class DevopsIngressControllerSpec extends Specification {

    @Autowired
    private TestRestTemplate restTemplate
    @Autowired
    private DevopsIngressMapper devopsIngressMapper
    @Autowired
    private DevopsProjectMapper devopsProjectMapper
    @Autowired
    private DevopsServiceMapper devopsServiceMapper
    @Autowired
    private DevopsEnvCommandMapper devopsEnvCommandMapper
    @Autowired
    private DevopsProjectService devopsProjectRepository
    @Autowired
    private DevopsEnvironmentMapper devopsEnvironmentMapper
    @Autowired
    private DevopsIngressPathMapper devopsIngressPathMapper
    @Autowired
    private DevopsCertificationMapper devopsCertificationMapper
    @Autowired
    private DevopsEnvFileResourceMapper devopsEnvFileResourceMapper
    @Autowired
    private DevopsEnvCommandService devopsEnvCommandRepository

    @Autowired
    @Qualifier("mockClusterConnectionHandler")
    private ClusterConnectionHandler envUtil
    @Autowired
    @Qualifier("mockGitUtil")
    private GitUtil gitUtil

    @Qualifier("mockBaseServiceClientOperator")
    @Autowired
    private BaseServiceClientOperator mockBaseServiceClientOperator

    @Qualifier("mockGitlabServiceClientOperator")
    @Autowired
    private GitlabServiceClientOperator mockGitlabServiceClientOperator

    @Autowired
    private GitlabGroupMemberService gitlabGroupMemberService

    DevopsEnvironmentService devopsEnvironmentService = Mockito.mock(DevopsEnvironmentService.class)
    @Shared
    DevopsIngressVO devopsIngressDTO = new DevopsIngressVO()
    @Shared
    DevopsIngressPathVO devopsIngressPathDTO = new DevopsIngressPathVO()
    @Shared
    DevopsEnvironmentDTO devopsEnvironmentDO = new DevopsEnvironmentDTO()
    @Shared
    DevopsServiceDTO devopsServiceDO = new DevopsServiceDTO()
    @Shared
    DevopsIngressDTO devopsIngressDO = new DevopsIngressDTO()
    @Shared
    DevopsEnvCommandDTO devopsEnvCommandDO = new DevopsEnvCommandDTO()
    @Shared
    DevopsIngressPathDTO devopsIngressPathDO = new DevopsIngressPathDTO()
    @Shared
    DevopsEnvFileResourceDTO devopsEnvFileResourceDO = new DevopsEnvFileResourceDTO()

    def setupSpec() {
        FileUtil.copyFile("src/test/gitops/org/pro/env/test-ing.yaml", "gitops/org/pro/env")
        devopsEnvironmentDO.setId(1L)
        devopsEnvironmentDO.setCode("env")
        devopsEnvironmentDO.setActive(true)
        devopsEnvironmentDO.setProjectId(1L)
        devopsEnvironmentDO.setClusterId(1L)
        devopsEnvironmentDO.setDevopsEnvGroupId(1L)
        devopsEnvironmentDO.setGitlabEnvProjectId(1L)

        devopsIngressPathDTO.setServiceId(1L)
        devopsIngressPathDTO.setPath("/bootz")
        devopsIngressPathDTO.setServicePort(7777L)
        devopsIngressPathDTO.setServiceName("test")
        devopsIngressPathDTO.setServiceStatus("running")
        List<DevopsIngressPathVO> pathList = new ArrayList<>()
        pathList.add(devopsIngressPathDTO)

        devopsIngressDTO.setEnvId(1L)
        devopsIngressDTO.setCertId(1L)
        devopsIngressDTO.setName("test.ing")
        devopsIngressDTO.setPathList(pathList)
        devopsIngressDTO.setDomain("test.test.com")

        devopsServiceDO.setId(1L)
        devopsServiceDO.setEnvId(1L)
        devopsServiceDO.setAppServiceId(1L)
        devopsServiceDO.setName("svc")
        devopsServiceDO.setCommandId(1L)
        devopsServiceDO.setStatus("running")
        devopsServiceDO.setType("ClusterIP")
        devopsServiceDO.setExternalIp("1.1.1.1")
        devopsServiceDO.setPorts("[{\"port\":7777}]")

        devopsIngressDO.setId(1L)
        devopsIngressDO.setEnvId(1L)
        devopsIngressDO.setCertId(1L)
        devopsIngressDO.setUsable(true)
        devopsIngressDO.setName("ingdo")
        devopsIngressDO.setCommandId(1L)
        devopsIngressDO.setProjectId(1L)
        devopsIngressDO.setStatus("running")
        devopsIngressDO.setCommandType("create")
        devopsIngressDO.setObjectVersionNumber(1L)
        devopsIngressDO.setCommandStatus("success")
        devopsIngressDO.setDomain("test.test.com")

        devopsEnvCommandDO.setId(1L)
        devopsEnvCommandDO.setStatus("success")
        devopsEnvCommandDO.setCommandType("create")

        devopsIngressPathDO.setId(1L)
        devopsIngressPathDO.setIngressId(1L)
        devopsIngressPathDO.setServiceId(1L)
        devopsIngressPathDO.setPath("testpath")

        devopsEnvFileResourceDO.setId(1L)
        devopsEnvFileResourceDO.setEnvId(1L)
        devopsEnvFileResourceDO.setResourceId(1L)
        devopsEnvFileResourceDO.setResourceType("Ingress")
        devopsEnvFileResourceDO.setFilePath("test-ing.yaml")
    }

    def setup() {
        devopsEnvironmentMapper.insert(devopsEnvironmentDO)
        devopsServiceMapper.insert(devopsServiceDO)
        devopsIngressMapper.insert(devopsIngressDO)
        devopsIngressPathMapper.insert(devopsIngressPathDO)
        devopsEnvCommandMapper.insert(devopsEnvCommandDO)
        devopsEnvFileResourceMapper.insert(devopsEnvFileResourceDO)

        ProjectDTO projectDO = new ProjectDTO()
        projectDO.setId(1L)
        projectDO.setCode("pro")
        projectDO.setOrganizationId(1L)
        Mockito.doReturn(projectDO).when(mockBaseServiceClientOperator).queryIamProjectById(eq(1L))

        OrganizationDTO organizationDO = new OrganizationDTO()
        organizationDO.setId(1L)
        organizationDO.setCode("org")
        Mockito.doReturn(organizationDO).when(mockBaseServiceClientOperator).queryOrganizationById(eq(1L))

        MemberDTO memberDO = new MemberDTO()
        memberDO.setAccessLevel(AccessLevel.OWNER.toValue())
        Mockito.when(mockGitlabServiceClientOperator.queryGroupMember(anyInt(), anyInt())).thenReturn(memberDO)

        List<RoleVO> roleDTOList = new ArrayList<>()
        RoleVO roleDTO = new RoleVO()
        roleDTO.setCode("role/project/default/project-owner")
        roleDTOList.add(roleDTO)
        List<ProjectWithRoleVO> projectWithRoleDTOList = new ArrayList<>()
        ProjectWithRoleVO projectWithRoleDTO = new ProjectWithRoleVO()
        projectWithRoleDTO.setName("pro")
        projectWithRoleDTO.setRoles(roleDTOList)
        projectWithRoleDTOList.add(projectWithRoleDTO)
        Mockito.doReturn(projectWithRoleDTOList).when(mockBaseServiceClientOperator).listProjectWithRoleDTO(anyLong())
    }


    def cleanup() {
        // 删除cert
        List<CertificationDTO> list = devopsCertificationMapper.selectAll()
        if (list != null && !list.isEmpty()) {
            for (CertificationDTO e : list) {
                devopsCertificationMapper.delete(e)
            }
        }
        // 删除env
        List<DevopsEnvironmentDTO> list1 = devopsEnvironmentMapper.selectAll()
        if (list1 != null && !list1.isEmpty()) {
            for (DevopsEnvironmentDTO e : list1) {
                devopsEnvironmentMapper.delete(e)
            }
        }
        // 删除ingress
        List<DevopsIngressDTO> list2 = devopsIngressMapper.selectAll()
        if (list2 != null && !list2.isEmpty()) {
            for (DevopsIngressDTO e : list2) {
                devopsIngressMapper.delete(e)
            }
        }
        // 删除service
        List<DevopsServiceDTO> list3 = devopsServiceMapper.selectAll()
        if (list3 != null && !list3.isEmpty()) {
            for (DevopsServiceDTO e : list3) {
                devopsServiceMapper.delete(e)
            }
        }
        // 删除envCommand
        List<DevopsEnvCommandDTO> list4 = devopsEnvCommandMapper.selectAll()
        if (list4 != null && !list4.isEmpty()) {
            for (DevopsEnvCommandDTO e : list4) {
                devopsEnvCommandMapper.delete(e)
            }
        }
        // 删除envFileResource
        List<DevopsEnvFileResourceDTO> list5 = devopsEnvFileResourceMapper.selectAll()
        if (list5 != null && !list5.isEmpty()) {
            for (DevopsEnvFileResourceDTO e : list5) {
                devopsEnvFileResourceMapper.delete(e)
            }
        }
        // 删除ingressPath
        List<DevopsIngressPathDTO> list6 = devopsIngressPathMapper.selectAll()
        if (list6 != null && !list6.isEmpty()) {
            for (DevopsIngressPathDTO e : list6) {
                devopsIngressPathMapper.delete(e)
            }
        }

    }

    def "Create"() {
        given: '初始化参数'
        CertificationDTO certificationDO = new CertificationDTO()
        certificationDO.setId(1L)
        certificationDO.setName("cert")
        certificationDO.setStatus(CertificationStatus.ACTIVE.getStatus())
        devopsCertificationMapper.insert(certificationDO)

        and: 'mock envUtil'
        envUtil.checkEnvConnection(_ as Long) >> null

        when: '项目下创建域名'
        restTemplate.postForEntity("/v1/projects/1/ingress?envId=1", devopsIngressDTO, Object.class)

        then: '校验返回值'
        devopsIngressMapper.selectAll().get(0)["name"] == "ingdo"
    }

    def "Update"() {
        given: '初始化DTO类'
        devopsIngressPathDTO = new DevopsIngressPathVO()
        devopsIngressPathDTO.setPath("/bootz")
        devopsIngressPathDTO.setServiceId(1L)
        devopsIngressPathDTO.setServicePort(7777L)
        devopsIngressPathDTO.setServiceName("test")
        devopsIngressPathDTO.setServiceStatus("running")
        List<DevopsIngressPathVO> pathList = new ArrayList<>()
        pathList.add(devopsIngressPathDTO)
        // 修改后的DTO
        DevopsIngressVO newDevopsIngressDTO = new DevopsIngressVO()
        newDevopsIngressDTO.setId(1L)
        newDevopsIngressDTO.setEnvId(1L)
        newDevopsIngressDTO.setCertId(1L)
        newDevopsIngressDTO.setCertName("newcertname")
        newDevopsIngressDTO.setName("ingdo")
        newDevopsIngressDTO.setPathList(pathList)
        newDevopsIngressDTO.setDomain("test.test-test.test")

        envUtil.checkEnvConnection(_ as Long) >> null
        gitUtil.cloneBySsh(_ as String, _ as String, _ as String) >> null
        devopsEnvironmentService.checkEnv(any(DevopsEnvironmentDTO.class), any(UserAttrDTO.class))

        and: "mock handDevopsEnvGitRepository"
        envUtil.handDevopsEnvGitRepository(_ as Long, _ as String, _ as String) >> "src/test/gitops/org/pro/env"
        when: '项目下更新域名'
        restTemplate.put("/v1/projects/1/ingress/1", newDevopsIngressDTO, Object.class)

        then: '校验返回值'
        devopsIngressMapper.selectByPrimaryKey(1L).getDomain() == "test.test.com"
    }

    def "QueryDomainId"() {
        when: '项目下查询域名'
        def dto = restTemplate.getForObject("/v1/projects/1/ingress/1", DevopsIngressVO.class)

        then: '校验返回值'
        dto["domain"] == "test.test.com"
    }

    def "Delete"() {
        given: 'mock envUtil'
        envUtil.checkEnvConnection(_ as Long) >> null

        when: '项目下删除域名'
        restTemplate.delete("/v1/projects/1/ingress/1")

        then: '校验返回值'
        devopsEnvCommandRepository.baseQueryByObject("ingress", 1L).getCommandType() == "delete"
    }

    def "CheckName"() {
        when: '检查域名唯一性'
        boolean exist = restTemplate.getForObject("/v1/projects/1/ingress/check_name?name=test&env_id=1", Boolean.class)

        then: '校验返回值'
        exist
    }

    def "CheckDomain"() {
        when: '检查域名名称唯一性'
        def exist = restTemplate.getForObject("/v1/projects/1/ingress/check_domain?env_id=1&domain=test.test&path=testpath&id=1", Boolean.class)

        then: '校验返回值'
        exist
    }

    def "ListByEnv"() {
        given: '初始化请求头'
        String params = "{\"searchParam\": {},\"params\": []}"

        HttpHeaders headers = new HttpHeaders()
        headers.setContentType(MediaType.valueOf("application/jsonUTF-8"))
        HttpEntity<String> strEntity = new HttpEntity<String>(params, headers)

        List<Long> envList = new ArrayList<>()
        envList.add(1L)
        envList.add(2L)
        envUtil.getConnectedClusterList() >> envList
        envUtil.getUpdatedClusterList() >> envList

        when: '环境总览域名查询'
        def page = restTemplate.postForObject("/v1/projects/1/ingress/1/page_by_env", strEntity, PageInfo.class)

        then: '校验返回值'
        page.getTotal() == 1

        and: '清理数据'


        FileUtil.deleteDirectory(new File("gitops"))
    }
}
