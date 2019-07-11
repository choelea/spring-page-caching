## Cache Page in Spring Web Applications
It's used to do caching at filter level. Most codes are copied/modified based on ehcache codes. 

## Usage
```
<dependency>
    <groupId>com.github.choelea</groupId>
    <artifactId>spring-page-caching</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```
To use it in your Spring Web Application, you needs to create an filter like below. 

```
public class SimplePageCachingFilter  extends AbstractPageCachingFilter {

	
	public SimplePageCachingFilter() {
		
	}

	protected Boolean isCacheable(HttpServletRequest httpRequest) {
		return true;
	}
	// for spring boot application, it's easy to just inject one in constructor method
	private CacheManager getCacheManager() {
		return (CacheManager)SpringUtils.getBean("cacheManager");
	}
	
	/**
	 * The Key Used in Cache
	 * @param request
	 * @return
	 */
	@Override
	protected String calculateKey(HttpServletRequest request) {
		return  request.getServerName()+request.getRequestURI();
	}
	
	/**
	 * Cache Name Used in Spring Cache
	 * 
	 */
	@Override
	protected String getCacheName() {
		return cacheName;
	}
		 
	@Override
	public PageInfo getPageInfo(String cacheName, String key) {
		Cache cache = getCacheManager().getCache(cacheName);
        ValueWrapper element = cache.get(key);
		return element==null?null:(PageInfo)element.get();
	}
	@Override
	public void putPageInfo(String cacheName, String key, PageInfo pageInfo) {
		Cache cache = getCacheManager().getCache(cacheName);
		cache.put(key, pageInfo);
	}
}
```
I guess above information is already enough for an experienced Java developer, but you can refer to https://github.com/choelea/spring-redis-cache-quickstart as an example.

>  将你自己的项目发布到maven中央仓库, 参考： https://www.jianshu.com/p/8c3d7fb09bce