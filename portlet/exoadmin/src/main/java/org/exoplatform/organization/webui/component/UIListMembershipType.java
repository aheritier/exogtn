/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.organization.webui.component;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import java.io.Writer;
import java.util.List;

@ComponentConfig(events = {
   @EventConfig(listeners = UIListMembershipType.EditMembershipActionListener.class),
   @EventConfig(listeners = UIListMembershipType.DeleteMembershipActionListener.class, confirm = "UIListMembershipType.deleteMemberShip")})
@Serialized
public class UIListMembershipType extends UIContainer
{

   private static String[] USER_BEAN_FIELD = {"name", "createdDate", "modifiedDate", "description"};

   private static String[] USER_ACTION = {"EditMembership", "DeleteMembership"};

   public UIListMembershipType() throws Exception
   {
      UIGrid uiGrid = addChild(UIGrid.class, null, "UIGrid");
      uiGrid.configure("name", USER_BEAN_FIELD, USER_ACTION);
      uiGrid.getUIPageIterator().setId("UIListMembershipTypeIterator");
      loadData();
   }

   public UIComponent getViewModeUIComponent()
   {
      return null;
   }

   public String getName()
   {
      return "UIMembershipList";
   }

   @SuppressWarnings("unchecked")
   public void loadData() throws Exception
   {
      getChild(UIGrid.class).getUIPageIterator().setPageList(new FindMembershipTypesPageList(5));
   }

   public void processRender(WebuiRequestContext context) throws Exception
   {
      Writer w = context.getWriter();
      w.write("<div class=\"UIListMembershipType\">");
      renderChildren();
      w.write("</div>");
   }

   static public class EditMembershipActionListener extends EventListener<UIListMembershipType>
   {
      public void execute(Event<UIListMembershipType> event) throws Exception
      {
         UIListMembershipType uiMembership = event.getSource();
         String name = event.getRequestContext().getRequestParameter(OBJECTID);

         OrganizationService service = uiMembership.getApplicationComponent(OrganizationService.class);
         MembershipType mt = service.getMembershipTypeHandler().findMembershipType(name);
         if(mt.getDescription()==null)
         {
            mt.setDescription("");
         }
         UIMembershipManagement uiMembershipManager = uiMembership.getParent();
         UIMembershipTypeForm uiForm = uiMembershipManager.getChild(UIMembershipTypeForm.class);
         uiForm.setMembershipType(mt);
         if (mt == null)
         {
            UIApplication uiApp = event.getRequestContext().getUIApplication();
            uiApp.addMessage(new ApplicationMessage("UIMembershipTypeForm.msg.MembershipNotExist", new String[]{name}));
            uiMembership.loadData();
         }
      }
   }

   static public class DeleteMembershipActionListener extends EventListener<UIListMembershipType>
   {
      public void execute(Event<UIListMembershipType> event) throws Exception
      {
         UIListMembershipType uiMembership = event.getSource();
         String name = event.getRequestContext().getRequestParameter(OBJECTID);
         UIMembershipManagement membership = uiMembership.getParent();
         UIMembershipTypeForm uiForm = membership.findFirstComponentOfType(UIMembershipTypeForm.class);

         String existMembershipTypeName = uiForm.getMembershipTypeName();
         if (existMembershipTypeName != null && existMembershipTypeName.equals(name))
         {
            UIApplication uiApp = event.getRequestContext().getUIApplication();
            uiApp.addMessage(new ApplicationMessage("UIMembershipList.msg.InUse", null));
            return;
         }

         //  Check to see whether given membershiptype is mandatory or not
         UserACL acl = uiMembership.getApplicationComponent(UserACL.class);
         List<String> mandatories = acl.getMandatoryMSTypes();
         if (!mandatories.isEmpty() && mandatories.contains(name))
         {
            UIApplication uiApp = event.getRequestContext().getUIApplication();
            uiApp.addMessage(new ApplicationMessage("UIMembershipList.msg.DeleteMandatory", null));
            return;
         }

         OrganizationService service = uiMembership.getApplicationComponent(OrganizationService.class);
         MembershipType membershipType = service.getMembershipTypeHandler().findMembershipType(name);
         UIPageIterator pageIterator = uiMembership.getChild(UIGrid.class).getUIPageIterator();
         int currentPage = -1;
         if (membershipType != null)
         {
            currentPage = pageIterator.getCurrentPage();
            service.getMembershipTypeHandler().removeMembershipType(name, true);
            membership.deleteOptions(membershipType);
         }
         uiMembership.loadData();
         if (currentPage >= 0)
         {
            while (currentPage > pageIterator.getAvailablePage())
               currentPage--;
            pageIterator.setCurrentPage(currentPage);
         }
      }
   }

}
