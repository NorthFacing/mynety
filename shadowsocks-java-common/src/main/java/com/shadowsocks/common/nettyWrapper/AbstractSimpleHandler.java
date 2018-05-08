/**
 * MIT License
 * <p>
 * Copyright (c) Bob.Zhu
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
package com.shadowsocks.common.nettyWrapper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import static com.shadowsocks.common.constants.Constants.LOG_MSG;
import static org.apache.commons.lang3.ClassUtils.getSimpleName;

/**
 * 主要是覆写增加了LOG日志和channel关闭抽象方法
 *
 * @author Bob.Zhu
 * @Email 0haizhu0@gmail.com
 * @since v0.0.4
 */
@Slf4j
public abstract class AbstractSimpleHandler<I> extends SimpleChannelInboundHandler<I> {

  protected final long activeTime = System.currentTimeMillis();

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.info("[ {}{} ] [{}-channelActive] channel active...", ctx.channel(), LOG_MSG, getSimpleName(this));
    super.channelActive(ctx);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    channelClose(ctx);
    long connTime = System.currentTimeMillis() - activeTime;
    logger.info("[ {}{} ] [{}-channelInactive] channel inactive, channel closed, conn time: {}ms", ctx.channel(), LOG_MSG, getSimpleName(this), connTime);
  }

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    logger.debug("[ {}{} ] [{}-channelReadComplete] channelReadComplete...", ctx.channel(), LOG_MSG, getSimpleName(this));
    super.channelReadComplete(ctx);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    channelClose(ctx);
    logger.error("[ " + ctx.channel() + LOG_MSG + " ] " + getSimpleName(this) + " error", cause);
  }

  protected abstract void channelClose(ChannelHandlerContext ctx);

}
