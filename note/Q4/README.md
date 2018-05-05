# HTTP代理

## 编解码

http 的 tunnel 代理直接连接，去掉所有编解码器之后就可以进行盲转了，
但是 HTTP 代理，在进行转发的时候需要加上编解码器，这样 proxy 和 server 
进行数据交换的时候，才能够识别到正确的 HTTP 内容。

## HttpObjectAggregator
当 HTTP content 内容太大的时候，是不是会出现内容不全的情况？

好像会，改成分段模式吧：

* HttpRequest（1个）
* HttpContent（可能有 0~N 个 content）
* LastHttpContent（1个）

## 信息发送之后没反应
首先考虑编解码问题，怎么可以快速查看当前channel的所有handler？
