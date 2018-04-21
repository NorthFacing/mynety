/**
 * MIT License
 * <p>
 * Copyright (c) 2018 0haizhu0@gmail.com
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.shadowsocks.client.httpAdapter;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static com.shadowsocks.common.constants.Constants.MAX_CONTENT_LENGTH;

/**
 * @author 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public class HttpPipelineInitializer extends ChannelInitializer<SocketChannel> {

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    log.info(LOG_MSG + " Init http handler..." + ch);

    HttpServerCodec serverCodec = new HttpServerCodec();
    HttpObjectAggregator aggregator = new HttpObjectAggregator(MAX_CONTENT_LENGTH);

    // HttpServerCodec 相当于 HttpRequestDecoder && HttpResponseEncoder 一起的作用，
    ch.pipeline().addLast(serverCodec);
    log.debug("{} {} add handler: serverCodec", LOG_MSG, ch);
//    ch.pipeline().addLast(new HttpRequestDecoder());
//    ch.pipeline().addLast(new HttpResponseEncoder());
    // 自动聚合 HTTP 的消息片段，将最大的消息大小为 1024 KB 的 HttpObjectAggregator 添加 到 ChannelPipeline
    ch.pipeline().addLast(aggregator);
    log.debug("{} {} add handler: aggregator", LOG_MSG, ch);
    // 请求解析为HttpRequest之后的数据处理
    ch.pipeline().addLast(HttpProxyHandler.INSTANCE);
    log.debug("{} {} add handler: HttpProxyHandler", LOG_MSG, ch);
  }
}