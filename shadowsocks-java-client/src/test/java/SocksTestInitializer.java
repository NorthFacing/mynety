import com.shadowsocks.common.constants.Constants;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocksTestInitializer extends ChannelInitializer<SocketChannel> {

  private static final Logger logger = LoggerFactory.getLogger(SocksTestInitializer.class);

  @Override
  public void initChannel(SocketChannel ch) throws Exception {
    logger.info(Constants.LOG_MSG + ch);
    ch.pipeline().addLast(new Socks01InitHandler());
  }
}