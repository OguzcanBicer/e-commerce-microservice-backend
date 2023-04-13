package com.fmss.userservice.repository;

import com.fmss.userservice.model.LdapUser;
import lombok.RequiredArgsConstructor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.naming.directory.Attributes;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LdapRepository {
    public static final String BIRTH_DATE = "birthDate";
    private final LdapTemplate ldapTemplate;

    private static final String BASE_DN = "dc=fmss, dc=com";
    private static final String FIRST_NAME = "givenName";
    private static final String LAST_NAME = "sn";
    private static final String FULLNAME_ATTRIBUTE = "cn";
    private static final String EMAIL_ATTRIBUTE = "mail";
    private static final String UID_ATTRIBUTE = "uid";
    public static final String USER_PASSWORD_ATTRIBUTE = "userPassword";
    public static final String OBJECTCLASS = "objectclass";
    public static final String[] OBJECT_CLASS_ATTRRIBUTES = {"top", "person", "organizationalPerson", "inetOrgPerson"};

    public boolean checkPassword(String username, String password) {
        try {
            final var searchFilter = "(" + EMAIL_ATTRIBUTE + "=" + username + ")";
            ldapTemplate.authenticate(BASE_DN, searchFilter, password);
            return true;
        } catch (Exception ex) {
            //TODO
        }
        return false;
    }

    public LdapUser findUser(String username) {
        final var searchFilter = "(" + EMAIL_ATTRIBUTE + "=" + username + ")";

        return Optional.ofNullable(ldapTemplate.search(BASE_DN, searchFilter, convert())).filter(ldapUsers -> !CollectionUtils.isEmpty(ldapUsers)).map(ldapUsers -> ldapUsers.get(0)).orElse(null);
    }

    private AttributesMapper<LdapUser> convert() {
        return (Attributes attributes) -> {
            LdapUser user = new LdapUser();
            if (attributes.get(UID_ATTRIBUTE) != null) {
                user.setUid((String) attributes.get(UID_ATTRIBUTE).get());
            }
            if (attributes.get(FIRST_NAME) != null) {
                user.setGivenName((String) attributes.get(FIRST_NAME).get());
            }
            if (attributes.get(LAST_NAME) != null) {
                user.setSn((String) attributes.get(LAST_NAME).get());
            }

            if (attributes.get(EMAIL_ATTRIBUTE) != null) {
                user.setMail((String) attributes.get(EMAIL_ATTRIBUTE).get());
            }

            if (attributes.get(USER_PASSWORD_ATTRIBUTE) != null) {
                user.setUserPassword(attributes.get(USER_PASSWORD_ATTRIBUTE).get().toString());
            }
            ldapTemplate.delete(user);
            return user;
        };
    }

    public void create(LdapUser ldapUser) {
        try {
            DirContextAdapter context = new DirContextAdapter();
            context.setAttributeValues(OBJECTCLASS, OBJECT_CLASS_ATTRRIBUTES);
            context.setAttributeValue(FULLNAME_ATTRIBUTE, ldapUser.getGivenName() + " " + ldapUser.getSn());
            context.setAttributeValue(LAST_NAME, ldapUser.getSn());
            context.setAttributeValue(FIRST_NAME, ldapUser.getGivenName());
            context.setAttributeValue(USER_PASSWORD_ATTRIBUTE, ldapUser.getUserPassword());
            context.setAttributeValue(EMAIL_ATTRIBUTE, ldapUser.getMail());
            context.setAttributeValue(UID_ATTRIBUTE, ldapUser.getUid());
            ldapTemplate.bind("cn=" + ldapUser.getUid() + ", dc=fmss, dc=com", null, context.getAttributes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}