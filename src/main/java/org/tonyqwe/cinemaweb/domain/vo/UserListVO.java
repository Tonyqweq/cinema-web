package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户列表展示对象
 */
@Data
public class UserListVO {

    private List<UserVO> records;
    private long total;
    private int current;
    private int size;
}
