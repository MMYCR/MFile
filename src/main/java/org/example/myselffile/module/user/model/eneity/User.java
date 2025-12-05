package org.example.myselffile.module.user.model.eneity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

@Data
@TableName(value = "mfile_user" , autoResultMap = true)
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID=1L;

    @TableId(value = "id", type= IdType.AUTO)
    private Long id;

    @TableField(value ="`username`")
    private String username;

    @JsonIgnore
    @TableField(value ="`password`")
    private String password;

    @TableField(value= "createtime",fill = FieldFill.INSERT)
    private Date createtime;

    @TableField(value= "`updatetime`",fill = FieldFill.UPDATE)
    private Date updatetime;

    // 暂时弃用
    //   @TableField(value = "permissions",typeHandler = CollectionStrTypeHandler.class)
    //  private Set<String> Permissions;

    @TableField(value = "`enable`")
    private Boolean enable;

}

