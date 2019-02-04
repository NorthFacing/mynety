
# CA证书结构

* 证书 (Certificate)
    * 版本号 (Version Number)
    * 序列号 (Serial Number)
    * 签名算法 (Signature Algorithm ID)
    * 颁发者 (Issuer Name)
    * 证书有效期 (Validity period)
        * 此日期前无效 (Not Before)
        * 此日期后无效 (Not After)
    * 主题 (Subject name)
    * 主题公钥信息 (Subject Public Key Info)
        * 公钥算法 (Public Key Algorithm)
        * 主题公钥 (Subject Public Key)
    * 颁发者唯一身份信息（可选项） (Issuer Unique Identifier (optional))
    * 主题唯一身份信息（可选项） (Subject Unique Identifier (optional))
    * 扩展信息（可选项） (Extensions (optional))
        * ... 
* 证书签名算法 (Certificate Signature Algorithm)
* 数字签名 (Certificate Signature)

## 主题字段信息

* 一般的数字证书产品的主题通常含有如下字段：
    - O: 组织名称 (Organization Name)	，对于 SSL 证书，一般为网站域名；而对于代码签名证书则为申请单位名称；而对于客户端单位证书则为证书申请者所在单位名称；
    - CN: 常用名称 (Common Name)，对于 SSL 证书，一般为网站域名；而对于代码签名证书则为申请单位名称；而对于客户端证书则为证书申请者的姓名。
* 证书申请单位所在地：
    - L: 所在城市 (Locality)
    - ST: 所在省份 (State/Province)
    - C: 所在国家 (Country)，只能是国家字母缩写，如中国：CN
* 其他一些字段：
    - OU: 组织单位
    - E: 电子邮件 (Email)
    - G: 多个姓名字段
    - Description 字段: 介绍
    - Phone 字段：电话号码，格式要求 + 国家区号 城市区号 电话号码，如： +86 732 88888888
    - STREET 字段：地址
    - PostalCode 字段：邮政编码



# 原理

* 首先系统有内置的CA证书，或者自己生成CA根证书导入到系统信任的根证书列表
* 然后使用CA根证书，生成对应的各个网站的子证书
* 然后就可以使用MITM进行解析了，如果需要代理任意网站，那么需要使用根证书动态生成各个子证书

# 参考
* [wiki - X.509](https://zh.wikipedia.org/wiki/X.509)
* [wiki - X.509](https://en.wikipedia.org/wiki/X.509)
* [segmentfault - HTTPS详解](https://segmentfault.com/a/1190000011675421)
