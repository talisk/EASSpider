package com.jplus.spider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class LoginPage {

	private CloseableHttpClient client = LoginConstants
			.getCustomCloseableHttpClient();
	public static String UrlCode;// token
	public static String loginUrl;// 登录教务系统url
	private boolean hasLogin = false;

	public LoginPage() {
		client = client == null ? LoginConstants.getCustomCloseableHttpClient()
				: client;
		hasLogin = false;
	}

	public boolean login() {
		CloseableHttpResponse response = null;
		try {
			// 得到完整的登录url
			loginUrl = getLoginUrl();
			// 得到表单中__VIEWSTATE值
			String state = getLoginState();
			// 得到表单中的验证码
			String checkCode = getChcekCode();
			// 构造登录表单
			HttpEntity entity = this.buildLoginForm(state, checkCode);
			HttpPost post = new HttpPost(MessageFormat.format(
					LoginConstants.loginPostUrl, UrlCode));
			post.setEntity(entity);
			//发送post请求
			response = client.execute(post);
			//处理返回的结果
			int statusCode = response.getStatusLine().getStatusCode();
			Header[] locationHead = response.getHeaders("Location");
			response.close();
			//模拟登录跳转
			return forward(statusCode, locationHead);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean hasLogin() {
		return hasLogin;
	}

	/**
	 * 获取登陆页面表单的__VIEWSTATE值
	 * 
	 * @return 登陆页面__VIEWSTATE值
	 * @throws IOException
	 */
	private String getLoginState() throws IOException {
		HttpGet get = new HttpGet("http://www.jwgl.hbnu.edu.cn" + loginUrl);
		CloseableHttpResponse response = client.execute(get);
		System.out.println("GET:" + get.getURI());

		Document doc = Jsoup.parse(EntityUtils.toString(response.getEntity(),
				"gb2312"));
		response.close();
		return doc.getElementsByAttributeValue("name", "__VIEWSTATE").get(0)
				.attr("value");
	}

	/**
	 * 得到验证码
	 * 
	 * @return
	 * @throws IOException
	 */
	private String getChcekCode() throws IOException {
		HttpGet get = new HttpGet(MessageFormat.format(
				LoginConstants.checkCodeUrl, UrlCode));
		CloseableHttpResponse response = client.execute(get);
		System.out.println("GET:" + get.getURI());
		ImageIcon icon = new ImageIcon(EntityUtils.toByteArray(response
				.getEntity()));
		response.close();
		String checkCode = (String) JOptionPane.showInputDialog(null, "",
				"输入验证码", JOptionPane.INFORMATION_MESSAGE, icon, null, null);
		return checkCode;
	}

	/**
	 * 模拟登录跳转
	 * @param code
	 * @param location
	 * @return
	 * @throws IOException
	 */
	private boolean forward(int code, Header[] location) throws IOException {
		if (code != 302 || location == null) {
			return false;
		}
		HttpGet get = new HttpGet("http://www.jwgl.hbnu.edu.cn"
				+ location[0].getValue());
		CloseableHttpResponse response = client.execute(get);
		response.close();
		hasLogin = true;
		return true;
	}

	/**
	 * 得到完整的loginurl
	 * 
	 * @return
	 * @throws IOException
	 */
	private String getLoginUrl() throws IOException {
		HttpHead head = new HttpHead("http://www.jwgl.hbnu.edu.cn/");
		head.setConfig(RequestConfig.custom().setRedirectsEnabled(false)
				.build());
		CloseableHttpResponse response = client.execute(head);
		System.out.println("HEAD:" + head.getURI());
		Header[] heads = response.getAllHeaders();
		for (Header item : heads) {
			System.out.println(item.getName() + ":" + item.getValue());
		}
		String location = response.getHeaders("Location")[0].getValue();
		// 获取教务系统域名后的token
		UrlCode = location.substring(1, location.lastIndexOf("/"));
		return location;
	}

	/**
	 * 创建表单
	 * 
	 * @param state
	 * @param code
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private HttpEntity buildLoginForm(String state, String code)
			throws UnsupportedEncodingException {
		List<NameValuePair> data = new ArrayList<NameValuePair>();
		data.add(new BasicNameValuePair("__VIEWSTATE", state));
		data.add(new BasicNameValuePair("txtUserName", LoginConstants.user));
		data.add(new BasicNameValuePair("TextBox2", LoginConstants.password));
		data.add(new BasicNameValuePair("txtSecretCode", code));
		data.add(new BasicNameValuePair("RadioButtonList1", "部门"));
		data.add(new BasicNameValuePair("Button1", ""));
		data.add(new BasicNameValuePair("lbLanguage", ""));
		HttpEntity entity = new UrlEncodedFormEntity(data, "gb2312");
		return entity;
	}
}
