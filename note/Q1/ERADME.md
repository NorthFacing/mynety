# CPU占用过高的问题

打开client客户端，浏览器访问几个页面之后，就会出现CPU占用过高的问题。

```
$ top
$ ps aux | grep 48451
$ jstack 48451 > stack.txt
```

经过排查发现大部分线程卡在PacFilter中，主要的就是正则过滤执行时间过长。
后来发现是正则表达式写的有问题，具体正则规则后续分析：

* 有问题的正则
    - Pattern.matches("^(\\w?.?)+" + conf, domain)
* 改造后的正则
    - Pattern.matches("([a-z0-9]+[.])*" + conf, domain)



