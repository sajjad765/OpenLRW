package org.apereo.openlrw.admin.service;

import org.apereo.openlrw.MongoServerConfig;
import org.apereo.openlrw.OpenLRW;
import org.apereo.openlrw.admin.AdminUser;
import org.apereo.openlrw.admin.endpoint.input.UserDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author xchopin <xavier.chopin@univ-lorraine.fr>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {OpenLRW.class, MongoServerConfig.class})
@WebAppConfiguration
public class AdminUserServiceTest {

    @Autowired
    private AdminUserService adminUserService;

    @Test
    public void testCreateAdminUser() {
        UserDTO user = new UserDTO();
        user.setUsername("johndoe");
        user.setPassword("password");
        user.setEmailAddress("johndoe@apereo.org");
        user.setOrgId("25736262");
        user.setTenantId("6636542");
        AdminUser adminUser = adminUserService.createAdminUser(user);
        assertThat(adminUser.getId(), is(notNullValue()));
        assertThat(adminUser.getUsername(), is(equalTo("johndoe")));
        assertThat(adminUser.getEmailAddress(), is(equalTo("johndoe@apereo.org")));
        assertThat(adminUser.getOrgId(), is(equalTo("25736262")));
        assertThat(adminUser.getTenantId(), is(equalTo("6636542")));
    }


   /** @Test
    public void testAuthUser() {
        UserDTO user = new UserDTO();
        user.setUsername("sammy_doe2");
        user.setPassword("passwd_2");
        user.setEmailAddress("sammydoe_2@apereo.org");
        user.setOrgId("45736262");
        user.setTenantId("8636542");
        AdminUser adminUser = adminUserService.createAdminUser(user);
        AdminUser authenticUser = adminUserService.authenticateUser("sammy_doe2", "passwd_2");
        assertThat(authenticUser.getId(), is(notNullValue()));
        assertThat(authenticUser.getUsername(), is(equalTo("sammy_doe2")));
        assertThat(authenticUser.getEmailAddress(), is(equalTo("sammydoe_2@apereo.org")));
        assertThat(authenticUser.getOrgId(), is(equalTo("45736262")));
        assertThat(authenticUser.getTenantId(), is(equalTo("8636542")));
    }

    @Test(expected = BadCredentialsException.class)
    public void testNonAuthUser() {
        UserDTO user = new UserDTO();
        user.setUsername("sammy_doe2");
        user.setPassword("passwd_2");
        user.setEmailAddress("sammydoe_2@apereo.org");
        user.setOrgId("45736262");
        user.setTenantId("8636542");
        AdminUser adminUser = adminUserService.createAdminUser(user);
        AdminUser authenticUser = adminUserService.authenticateUser("sammy_doe2", "passwd_21");
    }*/

}
