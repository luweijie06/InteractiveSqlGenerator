# InteractiveSqlGenerator - IDEA 交互式SQL生成器插件

InteractiveSqlGenerator 是一款功能强大的 IntelliJ IDEA 插件，用于简化SQL查询和ORM方法的生成过程。它特别适用于使用MyBatis、MyBatis-Plus和JPA等ORM框架的Java项目。

## 主要特性

- 🔍 多ORM框架支持：兼容MyBatis、MyBatis-Plus和JPA
- 🧠 智能字段映射：自动匹配域类和数据库实体字段
- 🏷️ 多种SQL操作：支持SELECT、UPDATE、DELETE和INSERT
- 🛠️ 交互式UI：直观的字段选择和条件设置界面
- 🔤 代码生成：自动生成SQL查询或ORM方法代码

## 安装方法

1. 打开 IntelliJ IDEA
2. 进入 `Settings/Preferences` → `Plugins`
3. 搜索 "InteractiveSqlGenerator"
4. 点击 `Install` 安装
5. 重启 IntelliJ IDEA

## 使用方法

1. 在编辑器中右键点击或使用插件菜单
2. 选择 `Generate > Interactive SQL Generator`
3. 在弹出的对话框中选择ORM框架和SQL操作类型
4. 选择域类和对应的数据库实体类
5. 配置需要包含的字段、条件和连接方式
6. 点击 `Generate` 生成SQL或ORM方法

## 示例

假设有一个域类 `UserDTO` 和一个数据库实体类 `User`：

```java
public class UserDTO {
    private String username;
    private String email;
    private int age;
}

@TableName("user")
public class User {
    @TableId
    private Long id;
    private String username;
    private String email;
    private Integer age;
}
```

使用 InteractiveSqlGenerator 可以生成如下 MyBatis-Plus 方法：

```java
public List<User> selectUser(UserDTO entity) {
    if (entity == null) {
        throw new IllegalArgumentException("Entity must not be null");
    }
    if (StringUtils.isEmpty(entity.getUsername()) &&
        StringUtils.isEmpty(entity.getEmail()) &&
        entity.getAge() == null) {
        throw new IllegalArgumentException("At least one search criteria must be provided");
    }
    return this.lambdaQuery()
        .eq(StringUtils.isNotEmpty(entity.getUsername()), User::getUsername, entity.getUsername())
        .eq(StringUtils.isNotEmpty(entity.getEmail()), User::getEmail, entity.getEmail())
        .eq(entity.getAge() != null, User::getAge, entity.getAge())
        .list();
}
```

## 技术特点

1. 智能ORM识别
    - 自动适配不同ORM框架的特性
    - 生成符合框架规范的代码

2. 字段映射
    - 智能匹配同名字段
    - 支持自定义字段映射关系

3. 条件生成
    - 支持多种SQL条件（等于、大于、小于等）
    - 自动生成空值检查

4. 代码生成
    - 生成规范的Java代码
    - 保持代码格式和样式一致

## 参与贡献

欢迎提交 Issue 和 Pull Request 来帮助改进这个插件！

## 开源协议

本项目采用 Apache-2.0 协议开源 - 详见 [LICENSE](LICENSE) 文件

## 支持

如果你遇到任何问题或有任何疑问，请在 GitHub 项目页面提交 issue。