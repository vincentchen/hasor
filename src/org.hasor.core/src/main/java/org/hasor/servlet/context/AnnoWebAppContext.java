/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hasor.servlet.context;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.hasor.context.Environment;
import org.hasor.context.ModuleInfo;
import org.hasor.context.Settings;
import org.hasor.context.anno.context.AnnoAppContextSupportModule;
import org.hasor.context.environment.StandardEnvironment;
import org.hasor.servlet.binder.FilterPipeline;
import org.hasor.servlet.binder.SessionListenerPipeline;
import org.hasor.servlet.binder.support.ManagedErrorPipeline;
import org.hasor.servlet.binder.support.ManagedFilterPipeline;
import org.hasor.servlet.binder.support.ManagedServletPipeline;
import org.hasor.servlet.binder.support.ManagedSessionListenerPipeline;
import org.hasor.servlet.binder.support.WebApiBinderModule;
import org.hasor.servlet.context.provider.DefaultHttpServletRequestProvider;
import org.hasor.servlet.context.provider.DefaultHttpServletResponseProvider;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
/**
 * 
 * @version : 2013-7-16
 * @author ������ (zyc@byshell.org)
 */
public class AnnoWebAppContext extends AnnoAppContextSupportModule {
    private Provider<HttpServletRequest>  httpRequestProvider  = new DefaultHttpServletRequestProvider();
    private Provider<HttpServletResponse> httpResponseProvider = new DefaultHttpServletResponseProvider();
    //
    public AnnoWebAppContext(ServletContext servletContext) throws IOException {
        super("hasor-config.xml", servletContext);
    }
    public AnnoWebAppContext(String mainConfig, ServletContext servletContext) throws IOException {
        super(mainConfig, servletContext);
    }
    /**��ȡ{@link HttpServletRequest}��Provider����Provider��������ע�롣*/
    public Provider<HttpServletRequest> getHttpRequestProvider() {
        return httpRequestProvider;
    }
    /**����{@link HttpServletRequest}��Provider����Provider��������ע�롣<br/>
     * ע�⣺�÷�����{@link #start()}��������֮ǰ���ò�����Ч��*/
    public void setHttpRequestProvider(Provider<HttpServletRequest> httpRequestProvider) {
        this.httpRequestProvider = httpRequestProvider;
    }
    /**��ȡ{@link HttpServletResponse}��Provider����Provider��������ע�롣*/
    public Provider<HttpServletResponse> getHttpResponseProvider() {
        return httpResponseProvider;
    }
    /**����{@link HttpServletResponse}��Provider����Provider��������ע�롣<br/>
     * ע�⣺�÷�����{@link #start()}��������֮ǰ���ò�����Ч��*/
    public void setHttpResponseProvider(Provider<HttpServletResponse> httpResponseProvider) {
        this.httpResponseProvider = httpResponseProvider;
    }
    /**��ȡ{@link ServletContext}*/
    public ServletContext getServletContext() {
        if (this.getContext() instanceof ServletContext)
            return (ServletContext) this.getContext();
        else
            return null;
    }
    @Override
    protected Environment createEnvironment() {
        return new WebStandardEnvironment(this.getSettings(), this.getServletContext());
    }
    @Override
    protected Injector createInjector(Module[] guiceModules) {
        Module webModule = new Module() {
            @Override
            public void configure(Binder binder) {
                /*Bind*/
                binder.bind(ManagedErrorPipeline.class);
                binder.bind(ManagedServletPipeline.class);
                binder.bind(FilterPipeline.class).to(ManagedFilterPipeline.class);
                binder.bind(SessionListenerPipeline.class).to(ManagedSessionListenerPipeline.class);
                /*��ServletContext�����Provider*/
                binder.bind(ServletContext.class).toProvider(new Provider<ServletContext>() {
                    @Override
                    public ServletContext get() {
                        return getServletContext();
                    }
                });
                /*��ServletRequest��HttpServletRequest�����Provider*/
                if (httpRequestProvider != null) {
                    binder.bind(ServletRequest.class).to(HttpServletRequest.class);
                    binder.bind(HttpServletRequest.class).toProvider(httpRequestProvider);
                    /*��HttpSession�����Provider*/
                    binder.bind(HttpSession.class).toProvider(new Provider<HttpSession>() {
                        public HttpSession get() {
                            return ((HttpServletRequest) httpResponseProvider.get()).getSession(true);
                        }
                    });
                }
                /*��ServletResponse��HttpServletResponse�����Provider*/
                if (httpResponseProvider != null) {
                    binder.bind(ServletResponse.class).to(HttpServletResponse.class);
                    binder.bind(HttpServletResponse.class).toProvider(httpResponseProvider);
                }
            }
        };
        //2.
        ArrayList<Module> guiceModuleSet = new ArrayList<Module>();
        guiceModuleSet.add(webModule);
        if (guiceModules != null)
            for (Module mod : guiceModules)
                guiceModuleSet.add(mod);
        return super.createInjector(guiceModuleSet.toArray(new Module[guiceModuleSet.size()]));
    }
    @Override
    protected WebApiBinderModule newApiBinder(final ModuleInfo forModule, final Binder binder) {
        return new WebApiBinderModule(this, forModule) {
            @Override
            public Binder getGuiceBinder() {
                return binder;
            }
        };
    }
}
/**
 * ����ע��MORE_WEB_ROOT���������Լ�Web����������ά����
 * @version : 2013-7-17
 * @author ������ (zyc@byshell.org)
 */
class WebStandardEnvironment extends StandardEnvironment {
    private ServletContext servletContext;
    public WebStandardEnvironment(Settings settings, ServletContext servletContext) {
        super(settings);
        this.servletContext = servletContext;
    }
    @Override
    protected Map<String, String> configEnvironment() {
        Map<String, String> hasorEnv = super.configEnvironment();
        hasorEnv.put("HASOR_WEBROOT", servletContext.getRealPath("/"));
        return hasorEnv;
    }
}