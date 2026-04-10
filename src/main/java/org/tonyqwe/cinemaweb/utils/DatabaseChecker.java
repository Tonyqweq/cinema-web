package org.tonyqwe.cinemaweb.utils;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.mapper.RoleMapper;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "org.tonyqwe.cinemaweb")
public class DatabaseChecker {

    public static void main(String[] args) {
        ApplicationContext context = new AnnotationConfigApplicationContext(DatabaseChecker.class);
        RoleMapper roleMapper = context.getBean(RoleMapper.class);
        
        System.out.println("=== 数据库角色表检查 ===");
        List<SysRole> roles = roleMapper.selectList(null);
        for (SysRole role : roles) {
            System.out.println("角色ID: " + role.getId() + ", 角色名称: " + role.getName() + ", 描述: " + role.getDescription());
        }
        System.out.println("=== 检查完成 ===");
        
        ((AnnotationConfigApplicationContext) context).close();
        System.exit(0);
    }
}
