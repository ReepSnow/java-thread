package com.snow.example.thread.file;

import com.sfebiz.common.utils.log.LogBetter;
import com.sfebiz.common.utils.log.LogLevel;
import com.sfebiz.cooperator.constant.HttpConfig;
import com.sfebiz.cooperator.exception.HttpClientException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.IOUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author 王鹏涛
 * @date 2019年5月31日18:22:41
 */
public final class HttpUtil {


	public static final ContentType APPLICATION_FORM_URLENCODED = ContentType
			.create("application/vnd.ehking-v1.0+json",  Consts.UTF_8);

	//100个连接数
	private static final int MAX_CONNECTION_SIZE = 200;
	private static final long KEEP_ALIVE_TIME = 10000;
	private static final int SOCKET_TIMEOUT = 60000;
	private static final int CONNECTION_TIMEOUT = 20000;
	private static final int CONNECTION_REQUEST_TIMEOUT = 5000;
	private static final String LOCK="LOCK";
	private static CloseableHttpClient hc = null;
	private static RequestConfig rc = null;
	/**
	 * 提交数据
	 *
	 * @param httpUrl
	 * @param queryString
	 *            content post参数 key1=val1&key2=val2&key3=val3
	 * @return
	 * @throws Exception
	 */
	public static String getByHttp(String httpUrl, String queryString) throws Exception {
		try {
			if (httpUrl != null && !httpUrl.endsWith("?") && queryString != null && !queryString.startsWith("?")) {
				queryString = "?" + queryString;
			}
			String response = Request.Get(httpUrl + queryString).connectTimeout(HttpConfig.CONNECT_TIMEOUT)
					.socketTimeout(HttpConfig.SOCKET_TIMEOUT).execute().returnContent().toString();
			return response;
		} catch (IOException e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTP请求异常，url：", httpUrl)
					.addParm("请求数据：", queryString).setException(e).log();
			return null;
		}
	}

	/**
	 * Form 表单的方式提交数据
	 *
	 * @param httpUrl
	 * @param content
	 *            content post参数 key1=val1&key2=val2&key3=val3
	 * @return
	 * @throws Exception
	 */
	public static String postFormByHttp(String httpUrl, String content) throws Exception {
		try {
			String response = Request.Post(httpUrl).connectTimeout(HttpConfig.CONNECT_TIMEOUT)
					.socketTimeout(HttpConfig.SOCKET_TIMEOUT)
					.bodyString(content, ContentType.create("application/x-www-form-urlencoded", Consts.UTF_8))
					.execute().returnContent().toString();
			return response;
		} catch (IOException e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTP请求异常，url：", httpUrl)
					.addParm("请求数据：", content).setException(e).log();
			return null;
		}
	}

	/**
	 * Form 表单的方式提交数据
	 *
	 * @param httpsUrl
	 * @param content
	 *            content post参数 key1=val1&key2=val2&key3=val3
	 * @return
	 * @throws Exception
	 */
	public static String postFormByHttps(String httpsUrl, String content) throws Exception {
		String responseBody = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHttpClient();
			HttpPost httpPost = new HttpPost(httpsUrl);
			StringEntity bodyEntity = new StringEntity(content, ContentType.APPLICATION_FORM_URLENCODED);
			httpPost.setEntity(bodyEntity);
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(HttpConfig.SOCKET_TIMEOUT)
					.setConnectTimeout(HttpConfig.CONNECT_TIMEOUT).build();
			httpPost.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				responseBody = EntityUtils.toString(entity);
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}

		} catch (Exception e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
					.addParm("请求数据：", content).setException(e).log();
			responseBody = null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
							.addParm("请求数据：", content).setException(e).log();
					responseBody = null;
				}
			}
		}
		return responseBody;
	}

	
	public static String postJsonFormByHttps(String httpsUrl, String content) throws Exception {
		String responseBody = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHttpClient();
			HttpPost httpPost = new HttpPost(httpsUrl);
			StringEntity bodyEntity = new StringEntity(content, APPLICATION_FORM_URLENCODED);
			httpPost.setEntity(bodyEntity);
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(HttpConfig.SOCKET_TIMEOUT)
					.setConnectTimeout(HttpConfig.CONNECT_TIMEOUT).build();
			httpPost.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				responseBody = EntityUtils.toString(entity);
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}

		} catch (Exception e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
					.addParm("请求数据：", content).setException(e).log();
			responseBody = null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
							.addParm("请求数据：", content).setException(e).log();
					responseBody = null;
				}
			}
		}
		return responseBody;
	}

	/**
	 * 以 Http 协议 ，Post Json 数据
	 *
	 * @param httpUrl
	 * @param requestBodyJsonString
	 * @return
	 */
	public static String postJsonByHttp(String httpUrl, String requestBodyJsonString) {
		try {
			String response = Request.Post(httpUrl).connectTimeout(HttpConfig.CONNECT_TIMEOUT)
					.socketTimeout(HttpConfig.SOCKET_TIMEOUT)
					.bodyString(requestBodyJsonString, ContentType.APPLICATION_JSON).execute().returnContent()
					.toString();
			return response;
		} catch (IOException e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTP请求异常，url：", httpUrl)
					.addParm("请求JSON数据：", requestBodyJsonString).setException(e).log();
			return null;
		}
	}

	/**
	 * 以 Https 协议 ，Post Json 数据
	 *
	 * @param httpsUrl
	 * @param requestBodyJsonString
	 * @return
	 */
	public static String postJsonByHttps(String httpsUrl, String requestBodyJsonString) {
		String responseBody = null;
		CloseableHttpClient httpClient = null;
		try {
			httpClient = getHttpClient();
			HttpPost httpPost = new HttpPost(httpsUrl);
			StringEntity bodyEntity = new StringEntity(requestBodyJsonString, ContentType.APPLICATION_JSON);
			httpPost.setEntity(bodyEntity);
			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(HttpConfig.SOCKET_TIMEOUT)
					.setConnectTimeout(HttpConfig.CONNECT_TIMEOUT).build();
			httpPost.setConfig(requestConfig);
			CloseableHttpResponse response = httpClient.execute(httpPost);
			int status = response.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				HttpEntity entity = response.getEntity();
				responseBody = EntityUtils.toString(entity);
			} else {
				throw new ClientProtocolException("Unexpected response status: " + status);
			}

		} catch (Exception e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
					.addParm("请求JSON数据：", requestBodyJsonString).setException(e).log();
			responseBody = null;
		} finally {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
					LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTPS请求异常，url：", httpsUrl)
							.addParm("请求JSON数据：", requestBodyJsonString).setException(e).log();
					responseBody = null;
				}
			}
		}
		return responseBody;
	}

	/**
	 * 获取HTTPClient，支持Https的访问
	 *
	 * @return
	 */
	private static CloseableHttpClient getHttpClient() {
		RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder.<ConnectionSocketFactory> create();
		ConnectionSocketFactory plainSF = new PlainConnectionSocketFactory();
		registryBuilder.register("http", plainSF);
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			//如果需要证书
			//trustStore.load(new FileInputStream(new File("你的证书位置")), "123456".toCharArray());
			TrustStrategy anyTrustStrategy = new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
					return true;
				}
			};
			SSLContext sslContext = SSLContexts.custom().useTLS().loadTrustMaterial(trustStore, anyTrustStrategy)
					.build();
			LayeredConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext,
					SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			registryBuilder.register("https", sslSF);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		Registry<ConnectionSocketFactory> registry = registryBuilder.build();
		PoolingHttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager(registry);
		return HttpClientBuilder.create().setConnectionManager(connManager).build();
	}
	public static String getByHttpHeader(String httpUrl, String queryString) throws Exception {
		if (httpUrl != null && !httpUrl.endsWith("?") && null != queryString && !queryString.startsWith("?")) {
			queryString = "?" + queryString;
		}
		if(null ==queryString){
			queryString="";
		}
		HttpGet req = new HttpGet(httpUrl+queryString);
		req.setHeader("Content-type", "application/json");
		req.setHeader("charset", "UTF-8");
		req.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(HttpConfig.SOCKET_TIMEOUT)
				.setConnectTimeout(HttpConfig.CONNECT_TIMEOUT).build();
		req.setConfig(requestConfig);
		try {
			HttpResponse res = HttpClients.createDefault().execute(req);
			String result = "";
			if (200 == res.getStatusLine().getStatusCode()) {
				result = EntityUtils.toString(res.getEntity());
			}
			return  result;
		}catch(IOException e) {
			LogBetter.instance(logger).setLevel(LogLevel.ERROR).addParm("HTTP请求异常，url：", httpUrl)
					.addParm("请求数据：", queryString).setException(e).log();

			throw new HttpClientException("http服务访问异常");
		}
	}
	public static InputStream getPromotion(String httpUrl) throws Exception {

		HttpGet req = new HttpGet(httpUrl);
		InputStream in=null;
		req.setHeader("Content-type", "application/json");
		req.setHeader("charset", "UTF-8");
		req.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000)
				.setConnectTimeout(120000).build();
		req.setConfig(requestConfig);
		try {
			HttpResponse res = HttpClients.createDefault().execute(req);

			int status = res.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				in =res.getEntity().getContent();
			} else {
				throw new ClientProtocolException("获取活动文件流http出现异常,Unexpected response status: " + status);
			}

		} catch (Exception e) {
			throw e;
		}
		return in;
	}
	public static InputStream getImage(String httpUrl) throws Exception {

		HttpGet req = new HttpGet(httpUrl);
		InputStream in=null;
		req.setHeader("Content-type", "application/json");
		req.setHeader("charset", "UTF-8");
		req.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36");
		rc = RequestConfig.custom().setCookieSpec("ignoreCookies").setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONNECTION_TIMEOUT).setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT).setExpectContinueEnabled(false).build();

		req.setConfig(rc);
		HttpResponse res=null;
		ByteArrayOutputStream outStream=null;
		try {
			res = getHttpClientForMoreConnectCount().execute(req);
			int status = res.getStatusLine().getStatusCode();
			if (status == HttpStatus.SC_OK) {
				outStream = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int len = 0;
				while( (len=res.getEntity().getContent().read(buffer)) != -1 ){
					outStream.write(buffer, 0, len);
				}
				in = new ByteArrayInputStream(outStream.toByteArray());
			} else {
				throw new ClientProtocolException("获取图片流http出现异常， response status: " + status);
			}
		} catch (Exception e) {
			throw e;
		}finally {
			if(null != res ){
				res.getEntity().getContent().close();
				IOUtils.closeQuietly(outStream);
			}
		}
		return in;
	}
	/**
	 * 接口请求服务
	 * @return
	 */
	private static CloseableHttpClient getHttpClientForMoreConnectCount() {
		if(hc == null) {
			synchronized(LOCK) {
				if(hc == null) {
					PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
					cm.setMaxTotal(MAX_CONNECTION_SIZE);
					cm.setDefaultMaxPerRoute(MAX_CONNECTION_SIZE);
					cm.setDefaultConnectionConfig(ConnectionConfig.custom().setCharset(Consts.UTF_8).build());
					hc = HttpClients.custom().setConnectionManager(cm).setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy() {
						@Override
						public long getKeepAliveDuration(HttpResponse arg0, HttpContext arg1) {
							long keepAlive = super.getKeepAliveDuration(arg0, arg1);
							if (keepAlive == -1) {
								// Keep connections alive 5 seconds if a keep-alive value
								// has not be explicitly set by the server
								keepAlive = KEEP_ALIVE_TIME;
							}
							return keepAlive;
						}
					}).build();
				}
			}
		}
		return hc;
	}
}
