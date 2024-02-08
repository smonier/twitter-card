package org.jahia.se.modules.twittercard;

import net.htmlparser.jericho.*;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.jahia.services.render.filter.RenderFilter;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.List;

@Component(service = RenderFilter.class)
public class TwitterCardFilter  extends AbstractFilter {
    public static final Logger logger = LoggerFactory.getLogger(TwitterCardFilter.class);

    private final static String TWITTERCARD_MODULE="twitter-card";
    private final static String TWITTERCARD_MIXIN="jmix:twitterCard";

    @Activate
    public void activate() {
        setPriority(0);// -1 launch after addStuff
        setApplyOnModes("live");//,preview
//        setApplyOnConfigurations("page");
        setApplyOnTemplateTypes("html");
        setSkipOnConfigurations("include,wrapper");//?
    }

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        String output = super.execute(previousOut, renderContext, resource, chain);
        boolean isInstalled = isInstalledModule(renderContext,TWITTERCARD_MODULE);
        JCRNodeWrapper currentNode = renderContext.getMainResource().getNode();
        boolean isEnabled =  currentNode.isNodeType(TWITTERCARD_MIXIN);
        //Disable the filter in case we are in Content Editor preview.
        boolean isCEPreview = renderContext.getRequest().getAttribute("ce_preview") != null;

        if(isInstalled && isEnabled && !isCEPreview){

            //update output to add scripts
            output = enhanceOutput(output, renderContext);
        }

        return output;
    }

        /**
     * This Function is just to add some logic to our filter and therefore not needed to declare a filter
     *
     * @param output    Original output to modify
     * @return          Modified output
     */
    @NotNull
    private String enhanceOutput(String output, RenderContext renderContext) throws Exception{
        Source source = new Source(output);
        OutputDocument outputDocument = new OutputDocument(source);

        //Add webapp script to the HEAD tag
        List<Element> elementList = source.getAllElements(HTMLElementName.HEAD);
        if (elementList != null && !elementList.isEmpty()) {
//            final StartTag headStartTag = elementList.get(0).getStartTag();
//            outputDocument.insert(headStartTag.getEnd(),getHeadScript(renderContext));
            final EndTag headEndTag = elementList.get(0).getEndTag();
            String ogScript = getHeadScript(renderContext);
            outputDocument.insert(headEndTag.getBegin()-1,ogScript);
        }

        output = outputDocument.toString().trim();
        return output;
    }

    private String getHeadScript(RenderContext renderContext) throws RepositoryException, IOException {

        String hostname = getHostname(renderContext);

        JCRNodeWrapper currentNode = renderContext.getMainResource().getNode();
        String twitterCard = currentNode.getPropertyAsString("social:twitterCardType");
        String twitterSite = currentNode.getPropertyAsString("social:twitterSite");
        String twitterTitle = currentNode.getPropertyAsString("social:twitterTitle");
        String twitterDescription = currentNode.getPropertyAsString("social:twitterDescription");
        String twitterCreator = currentNode.getPropertyAsString("social:twitterCreator");
        String twitterAppIdiPhone = currentNode.getPropertyAsString("social:twitterAppIdiPhone");
        String twitterAppIdiPad = currentNode.getPropertyAsString("social:twitterAppIdiPad");
        String twitterAppIdGooglePlay = currentNode.getPropertyAsString("social:twitterAppIdGooglePlay");
        String twitterPlayer = currentNode.getPropertyAsString("social:twitterPlayer");
        String twitterPlayerWidth = currentNode.getPropertyAsString("social:twitterPlayerWidth");
        String twitterPlayerHeight = currentNode.getPropertyAsString("social:twitterPlayerHeight");


        StringBuilder headScriptBuilder = new StringBuilder("\n<meta name=\"twitter:card\" content=\""+twitterCard+"\" />");
        if(twitterTitle!=null && !twitterTitle.isEmpty()){
            headScriptBuilder.append("\n<meta name=\"twitter:title\" content=\""+twitterTitle+"\" />");
        } else {
            headScriptBuilder.append("\n<meta name=\"twitter:title\" content=\""+currentNode.getDisplayableName()+"\" />");
        }
        if(twitterDescription!=null && !twitterDescription.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:description\" content=\""+twitterDescription+"\" />");
        if(twitterSite!=null && !twitterSite.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:site\" content=\""+twitterSite+"\" />");
        if(twitterCreator!=null && !twitterCreator.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:creator\" content=\""+twitterCreator+"\" />");
        if(twitterAppIdiPhone!=null && !twitterAppIdiPhone.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:app:id:iphone\" content=\""+twitterAppIdiPhone+"\" />");
        if(twitterAppIdiPad!=null && !twitterAppIdiPad.isEmpty())
            headScriptBuilder.append("\n<meta name=\"ttwitter:app:id:ipad\" content=\""+twitterAppIdiPad+"\" />");
        if(twitterAppIdGooglePlay!=null && !twitterAppIdGooglePlay.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:app:id:googleplay\" content=\""+twitterAppIdGooglePlay+"\" />");
        if(twitterPlayer!=null && !twitterPlayer.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:player\" content=\""+twitterPlayer+"\" />");
        if(twitterPlayerWidth!=null && !twitterPlayerWidth.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:player:width\" content=\""+twitterPlayerWidth+"\" />");       
        if(twitterPlayerHeight!=null && !twitterPlayerHeight.isEmpty())
            headScriptBuilder.append("\n<meta name=\"twitter:player:height\" content=\""+twitterPlayerHeight+"\" />");       
        
            try {
            JCRNodeWrapper twitterImage = (JCRNodeWrapper) currentNode.getProperty("social:twitterImage").getNode();
            if(twitterImage != null){
                String alt = twitterImage.getDisplayableName();
                headScriptBuilder.append("\n<meta name=\"twitter:image\" content=\""+hostname+twitterImage.getUrl()+"\" />");
                headScriptBuilder.append("\n<meta name=\"twitter:image:alt\" content=\""+alt+"\" />");
            }
        }catch (Exception e){
            logger.info("no image selected in Twitter Card for content : "+currentNode.getDisplayableName());
        }

        return headScriptBuilder.toString();
    }

    private boolean isInstalledModule(RenderContext renderContext, String moduleName) throws RepositoryException {
        boolean isInstalled = false;
        JCRPropertyWrapper installedModules = renderContext.getSite().getProperty("j:installedModules");
        for (JCRValueWrapper module : installedModules.getValues()) {
            if (moduleName.equals(module.getString())) {
                isInstalled = true;
                break;
            }
        }
        return isInstalled;
    }

    private String getHostname (RenderContext renderContext) {
        String schema = renderContext.getRequest().getScheme();
        String host = renderContext.getRequest().getServerName();
        int port = renderContext.getRequest().getServerPort();
        String hostname = schema+"://"+host;
        if(port!= 80 && port!= 443){
            hostname = hostname + ":" + String.valueOf(port);
        }
        return hostname;
    }

}
