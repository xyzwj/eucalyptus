/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2008, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the
 *   following disclaimer in the documentation and/or other
 *   materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Author: Dmitrii Zagorodnov dmitrii@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.admin.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: dmitriizagorodnov
 * Date: May 3, 2008
 * Time: 2:57:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class EucalyptusWebInterface implements EntryPoint {

    private static String cookie_name = "eucalyptus-session-id";
    private static int minPasswordLength = 5;  /* TODO: put into config? */

    /* configuration parameters to be set from the server */
    private static Boolean server_ready = new Boolean(false);
    private static String login_greeting;
    private static String signup_greeting;
    private static String cloud_name;
    private static String certificate_download_text;
    private static String rest_credentials_text;
    private static String user_account_text;
    private static String admin_email_change_text;
	private static String admin_walrus_setup_text;
    private static boolean request_telephone;
    private static boolean request_project_leader;
    private static boolean request_affiliation;
    private static boolean request_project_description;
    private static Image logo = null;

    /* global variables */
    private static HashMap props;
    private static HashMap urlParams;
    private static String sessionId;
    private static String currentAction;
    private static UserInfoWeb loggedInUser;
	private static TabBar allTabs;
    private static int currentTabIndex = 0;
	private static int credsTabIndex;
    private static int imgTabIndex;
	private static int usrTabIndex;
	private static int confTabIndex;

    /* globally visible UI widgets */
    private Label label_box = new Label();
    private CheckBox check_box = new CheckBox("", false);
    private Label remember_label = new Label("Remember me on this computer");

    public void onModuleLoad()
    {
        sessionId = Cookies.getCookie( cookie_name );
        urlParams = GWTUtils.parseParamString( GWTUtils.getParamString() );

        /* if specified, 'page' will tell us which tab to select */
        String page = ( String ) urlParams.get( "page" );
        if (page!=null) { currentTabIndex = Integer.parseInt(page); }

        currentAction = ( String ) urlParams.get( "action" );

        displayStatusPage("Loading data from server...");
        EucalyptusWebBackend.App.getInstance().getProperties(
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        props = ( HashMap ) result;
                        try {
                            load_props(); /* verify properties */

                            /* if we have don't have sessionId saved in a cookie */
                            if ( sessionId == null )
                            {
								/* these don't need sessions */
								if ( currentAction.equals ("confirm")
								|| currentAction.equals ("recover") ) {
									executeAction( currentAction );
								} else {
									displayLoginPage();
								}
                            }
                            else /* we have a cookie - try using it */
                            {
                                check_box.setChecked(true);
                                attemptLogin();
                            }
                        } catch (Exception e) {
                            displayErrorPageFinal ("Internal error (1): " + e.getMessage());
                        }
                    }
                    public void onFailure( Throwable caught )
                    {
                        displayErrorPageFinal ("Internal error (2): " + caught.getMessage());
                    }
                }
        );
    }

    void load_props() throws Exception
    {
        if (props == null) {
            throw new Exception("Invalid server configuration");
        }
        cloud_name = (String)props.get("cloud-name");
        login_greeting = (String)props.get("login-greeting");
        signup_greeting = (String)props.get("signup-greeting");
        certificate_download_text = (String)props.get("certificate-download-text");
        rest_credentials_text = (String)props.get("rest-credentials-text");
        user_account_text = (String)props.get("user-account-text");
        admin_email_change_text = (String)props.get("admin-email-change-text");
		admin_walrus_setup_text = (String)props.get("admin-walrus-setup-text");
        server_ready = (Boolean)props.get("ready");

        if (server_ready==null) {
            throw new Exception("Internal server erorr (cannot determine server readiness)");
        }
        if (cloud_name==null) {
            throw new Exception("Server configuration is missing 'cloud-name' value");
        }
        if (login_greeting==null) {
            throw new Exception("Server configuration is missing 'login-greeting' value");
        }
        if (signup_greeting==null) {
            throw new Exception("Server configuration is missing 'signup-greeting' value");
        }
        if (certificate_download_text==null) {
            throw new Exception("Server configuration is missing 'certificate-dowload-text' value");
        }
        if (rest_credentials_text==null) {
            throw new Exception("Server configuration is missing 'rest-credentials-text' value");
        }
        if (user_account_text==null) {
            throw new Exception("Server configuration is missing 'user-account-text' value");
        }
        if (admin_email_change_text==null) {
            throw new Exception("Server configuration is missing 'admin-email-change-text' value");
        }
        if (admin_walrus_setup_text==null) {
            throw new Exception("Server configuration is missing 'admin-walrus-setup-text' value");
        }

        /* optional parameters (booleans will be 'yes' if not specified) */
        request_telephone = str2bool((String)props.get("request-telephone"));
        request_project_leader = str2bool((String)props.get("request-project-leader"));
        request_affiliation = str2bool((String)props.get("request-affiliation"));
        request_project_description = str2bool((String)props.get("request-project-description"));
        String logo_file = (String)props.get("logo-file");
        if (logo_file!=null) {
            logo = new Image(logo_file);
        }
    }

    private boolean str2bool(String s)
    {
        if (s==null) {
            return true;
        }
        if (s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("n")
                || s.equalsIgnoreCase("0")) {
            return false;
        } else {
            return true;
        }
    }

    public void displayLoginPage()
    {
        if ( currentAction == null ) {
            displayLoginPage(login_greeting);
        } else {
            if ( currentAction.equals( "approve" )
                    || currentAction.equals( "reject" )
                    || currentAction.equals( "disable" )
                    || currentAction.equals( "delete" )) {
                displayLoginPage("Please, log into Eucalyptus to " + currentAction + " the user");

            } else if ( currentAction.equals( "delete_image")) {
                displayLoginPage("Please, log into Eucalyptus to delete the image");

            } else if ( currentAction.equals( "confirm" ) ) {
                displayLoginPage("Please, log into Eucalyptus to confirm your acccount");

            } else { /* unknown action - will be caught upon login */
                displayLoginPage(login_greeting);
            }
            label_box.setStyleName ("euca-greeting-warning"); /* highlight the message */
        }
    }

    public void displayLoginPage(String greeting)
    {
        History.newItem("login");
        label_box.setText( greeting );
        label_box.setStyleName("euca-greeting-normal");
        final TextBox login_box = new TextBox();
        final PasswordTextBox pass_box = new PasswordTextBox();

        ClickListener LoginButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                /* perform checks */
                if ( login_box.getText().length() < 1 )
                {
                    displayLoginErrorPage("Username is empty!");
                    return;
                }
                if ( pass_box.getText().length() < 1 )
                {
                    displayLoginErrorPage("Password is empty!");
                    return;
                }

                label_box.setText("Contacting the server...");
                label_box.setStyleName("euca-greeting-pending");
                EucalyptusWebBackend.App.getInstance().getNewSessionID(
                        login_box.getText(),
                        GWTUtils.md5(pass_box.getText()),
                        new AsyncCallback() {
                            public void onSuccess( Object result )
                            {
                                sessionId = ( String ) result;
                                long expiresMs = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000); /* week */
                                Date expires = new Date(expiresMs);
                                if (check_box.isChecked()) {
                                    Cookies.setCookie( cookie_name, sessionId, expires);
                                } else {
                                    /* this cookie should expire at the end of the session */
                                    /* TODO: does this work right in all browsers? */
                                    Cookies.setCookie( cookie_name, sessionId, new Date(0));
                                }
                                attemptLogin();
                            }

                            public void onFailure( Throwable caught )
                            {
                                displayLoginErrorPage((String)caught.getMessage());
                            }
                        }
                );
            }
        };

        ClickListener RecoverButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                displayPasswordRecoveryPage();
            }
        };

        Button submit_button = new Button( "Sign in", LoginButtonListener );
        Hyperlink signup_button = new Hyperlink( "Apply", "apply" );
        signup_button.addClickListener( AddUserButtonListener );
        Hyperlink recover_button = new Hyperlink( "Recover", "recover" );
        recover_button.addClickListener( RecoverButtonListener );
        remember_label.setStyleName("euca-remember-text");

        Grid g = new Grid( 4, 2 );
        g.setCellSpacing(4);
        g.setWidget(0, 0, new Label("Username:"));
        g.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(1, 0, new Label("Password:"));
        g.getCellFormatter().setHorizontalAlignment(1, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(0, 1, login_box );
        g.setWidget(1, 1, pass_box );
        g.setWidget(2, 0, check_box);
        g.getCellFormatter().setHorizontalAlignment(2, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        g.setWidget(2, 1, remember_label);
        g.setWidget(3, 1, submit_button);
        VerticalPanel panel = new VerticalPanel();
        panel.add (g);
        panel.setStyleName("euca-login-panel");
        panel.setCellHorizontalAlignment(g, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(g, HasVerticalAlignment.ALIGN_MIDDLE);

        HorizontalPanel hpanel = new HorizontalPanel();
        hpanel.setSpacing(2);
        hpanel.add( signup_button );
        hpanel.add( new HTML("&nbsp;for account&nbsp;&nbsp;|&nbsp;&nbsp;") );
        hpanel.add( recover_button );
        hpanel.add( new HTML("&nbsp;password") );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (panel);
        if (server_ready.booleanValue()) {
            vpanel.add (hpanel);
        }

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

    public void displayLoginErrorPage ( String message )
    {
        if (message.equals("Earlier session not found") ||
                message.equals("Earlier session expired")) {
            displayLoginPage();
        } else {
            displayLoginPage("Error: " + message);
            label_box.setStyleName("euca-greeting-warning");
        }
    }

	/* this handles sign-ups, adding of users by admin, and editing of users */
    public void displayUserRecordPage( Panel parent, UserInfoWeb userToEdit)
    {
		final String oldPassword;
		final boolean newUser;
        boolean admin = false;

        if (loggedInUser != null
                && loggedInUser.isAdministrator().booleanValue()) {
             admin = true;
        }
		if (userToEdit==null) {
			newUser = true;
			userToEdit = new UserInfoWeb();
			oldPassword = "";
			if ( admin ) {
	            label_box.setText ("Please, fill out the form to add a user");
	        } else {
	            label_box.setText ( signup_greeting ); // Please, fill out the form:
	        }
		} else {
			newUser = false;
			oldPassword = userToEdit.getBCryptedPassword();
			label_box.setText ("Editing information for user '" + userToEdit.getUserName() +"'");
		}
        label_box.setStyleName("euca-greeting-normal");

        final Grid g1 = new Grid ( 5, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final Label label_mandatory = new Label( "Mandatory fields:" );
        label_mandatory.setStyleName("euca-section-header");

        final int userName_row = i;
        g1.setWidget( i, 0, new Label( "Username:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox userName_box = new TextBox();
		userName_box.setText (userToEdit.getUserName());
        userName_box.setWidth("180");
		if ( ! newUser ) {
			userName_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, userName_box );

        final int password1_row = i;
        g1.setWidget( i, 0, new Label( "Password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword1_box = new PasswordTextBox();
		cleartextPassword1_box.setText (userToEdit.getBCryptedPassword());
        cleartextPassword1_box.setWidth ("180");
		if ( (! admin && ! newUser ) || userToEdit.isAdministrator()) {
			cleartextPassword1_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, cleartextPassword1_box );

        final int password2_row = i;
        g1.setWidget( i, 0, new Label( "Password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword2_box = new PasswordTextBox();
		cleartextPassword2_box.setText (userToEdit.getBCryptedPassword());
        cleartextPassword2_box.setWidth("180");
		if ( ( ! admin && ! newUser ) || userToEdit.isAdministrator()) {
			cleartextPassword2_box.setEnabled (false);
		}
        g1.setWidget( i++, 1, cleartextPassword2_box );

        final int realName_row = i;
        g1.setWidget( i, 0, new Label( "Full Name:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox realName_box = new TextBox();
		realName_box.setText (userToEdit.getRealName());
        realName_box.setWidth("180");
        g1.setWidget( i++, 1, realName_box );

        final int emailAddress_row = i;
        g1.setWidget( i, 0, new Label( "Email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress_box = new TextBox();
		emailAddress_box.setText (userToEdit.getEmail());
        emailAddress_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress_box );

        /* these widgets are allocated, but not necessarily used */
        final Grid g2 = new Grid();
        final Label label_optional = new Label( "Optional fields:" );
        label_optional.setStyleName("euca-section-header");
        final TextBox telephoneNumber_box = new TextBox();
        final TextBox projectPIName_box = new TextBox();
        final TextBox affiliation_box = new TextBox();
        final TextArea projectDescription_box = new TextArea();

        int extra_fields = 0;
        if (request_telephone)           { extra_fields++; }
        if (request_project_leader)      { extra_fields++; }
        if (request_affiliation)         { extra_fields++; }
        if (request_project_description) { extra_fields++; }

        if (extra_fields > 0) {
            g2.resize(extra_fields, 2);
            g2.getColumnFormatter().setWidth(0, "180");
            g2.getColumnFormatter().setWidth(1, "360");
            i = 0;

            if (request_telephone) {
                g2.setWidget( i, 0, new Label( "Telephone Number:" ));
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
                telephoneNumber_box.setWidth("180");
				telephoneNumber_box.setText (userToEdit.getTelephoneNumber());
                g2.setWidget( i++, 1, telephoneNumber_box );
            }

            if (request_project_leader) {
                g2.setWidget( i, 0, new Label( "Project Leader:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				projectPIName_box.setText (userToEdit.getProjectPIName());
                projectPIName_box.setWidth("180");
                g2.setWidget( i++, 1, projectPIName_box );
            }

            if (request_affiliation) {
                g2.setWidget( i, 0, new Label( "Affiliation:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				affiliation_box.setText (userToEdit.getAffiliation());
                affiliation_box.setWidth("360");
                g2.setWidget( i++, 1, affiliation_box );
            }

            if (request_project_description) {
                g2.setWidget( i, 0, new Label( "Project Description:" ) );
                g2.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
				projectDescription_box.setText (userToEdit.getProjectDescription());
                projectDescription_box.setWidth("360");
                projectDescription_box.setHeight("50");
                g2.setWidget( i++, 1, projectDescription_box );
            }
        }

        ClickListener SignupButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 4; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( userName_box.getText().length() < 1 )
                {
                    Label l = new Label( "Username is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( userName_row, 2, l);
                    formOk = false;
                }
				/* no spaces in username */
				if ( userName_box.getText().matches(".*[ \t]+.*") ) {
					Label l = new Label ("Username cannot have spaces, sorry!");
					l.setStyleName ("euca-error-hint");
					g1.setWidget (userName_row, 2, l);
					formOk = false;
				}

                if ( cleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password must be at least " + minPasswordLength + " characters long!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( !cleartextPassword1_box.getText().equals( cleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password2_row, 2, l );
                    formOk = false;
                }
                // TODO: matches() won't work if user's input contains special regexp characters
                if ( realName_box.getText().toLowerCase().matches(".*" +
                        cleartextPassword1_box.getText().toLowerCase() + ".*") ){
                    Label l = new Label ( "Password may not contain parts of your name!");
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( cleartextPassword1_box.getText().toLowerCase().matches(".*" +
                        userName_box.getText().toLowerCase() + ".*")) {
                    Label l = new Label ( "Password may not contain the username!");
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( realName_box.getText().length() < 1 )
                {
                    Label l = new Label( "Name is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( realName_row, 2, l );
                    formOk = false;
                }
                if ( emailAddress_box.getText().length() < 1 )
                {
                    Label l = new Label( "Email address is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( emailAddress_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");
					String enteredPassword = cleartextPassword1_box.getText();
					String encryptedPassword = GWTUtils.md5(enteredPassword);
					if ( enteredPassword.equals(oldPassword)) {
						encryptedPassword = enteredPassword; // it was not changed in the edit
					}
                    final UserInfoWeb userToSave = new UserInfoWeb(
                            userName_box.getText(),
                            realName_box.getText(),
                            emailAddress_box.getText(),
                            encryptedPassword);
                    if ( telephoneNumber_box.getText().length() > 0 )
                    {
                        userToSave.setTelephoneNumber( telephoneNumber_box.getText() );
                    }
                    if ( affiliation_box.getText().length() > 0 )
                    {
                        userToSave.setAffiliation( affiliation_box.getText() );
                    }
                    if ( projectDescription_box.getText().length() > 0 )
                    {
                        userToSave.setProjectDescription( projectDescription_box.getText() );
                    }
                    if ( projectPIName_box.getText().length() > 0 )
                    {
                        userToSave.setProjectPIName( projectPIName_box.getText() );
                    }
					if (newUser) {
						EucalyptusWebBackend.App.getInstance().addUserRecord(
							sessionId, /* will be null if anonymous user signs up */
							userToSave,
						new AsyncCallback() {
							public void onSuccess( Object result )
							{
								displayDialog( "Thank you!", ( String ) result );
							}

							public void onFailure( Throwable caught )
							{
								String m = caught.getMessage();
								if ( m.equals( "User already exists" ) )
								{
									g1.setWidget( userName_row, 2, new Label( "Username is taken!" ) );
									label_box.setText( "Please, fix the error and resubmit:" );
									label_box.setStyleName("euca-greeting-warning");
								} else {
									displayErrorPage(m);
								}
							}
						}
						);
					} else {
						EucalyptusWebBackend.App.getInstance().updateUserRecord(
							sessionId,
							userToSave,
						new AsyncCallback() {
							public void onSuccess( Object result )
							{
								displayDialog( "", ( String ) result );
								loggedInUser.setRealName(userToSave.getRealName());
								loggedInUser.setEmail(userToSave.getEmail());
								loggedInUser.setBCryptedPassword(userToSave.getBCryptedPassword());
								loggedInUser.setTelephoneNumber(userToSave.getTelephoneNumber());
								loggedInUser.setAffiliation(userToSave.getAffiliation());
								loggedInUser.setProjectDescription(userToSave.getProjectDescription());
								loggedInUser.setProjectPIName(userToSave.getProjectPIName());
							}

							public void onFailure( Throwable caught )
							{
								String m = caught.getMessage();
								displayErrorPage(m);
							}
						}
						);
					}
                }
                else
                {
                    label_box.setText( "Please, fix the errors and resubmit:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

		Button submit_button;
		if (newUser) {
	        if (admin) {
				submit_button = new Button ( "Add user", SignupButtonListener);
			} else {
				submit_button = new Button ( "Sign up", SignupButtonListener);
			}
		} else {
			submit_button = new Button ( "Update Record", SignupButtonListener );
		}

        Button cancel_button = new Button( "Cancel", DefaultPageButtonListener );
        VerticalPanel mpanel = new VerticalPanel();
        mpanel.add( label_mandatory );
        mpanel.add( g1 );

        VerticalPanel opanel = new VerticalPanel();
        if (extra_fields > 0) {
            opanel.add( label_optional );
            opanel.add( g2 );
        }

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( submit_button );
        bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
        bpanel.add( cancel_button );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (mpanel);
        vpanel.add (opanel);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        parent.clear();
        parent.add( wrapper );
    }

    public void displayPasswordRecoveryPage()
    {
        label_box.setText ("Please, choose the new password");
        label_box.setStyleName("euca-greeting-normal");

        final Grid g1 = new Grid ( 3, 3 );
        g1.getColumnFormatter().setWidth(0, "230");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final int usernameOrEmail_row = i;
        g1.setWidget( i, 0, new Label( "Username OR email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox usernameOrEmail_box = new TextBox();
        usernameOrEmail_box.setWidth("180");
        g1.setWidget( i++, 1, usernameOrEmail_box );

        final int password1_row = i;
        g1.setWidget( i, 0, new Label( "New password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword1_box = new PasswordTextBox();
        cleartextPassword1_box.setWidth("180");
        g1.setWidget( i++, 1, cleartextPassword1_box );

        final int password2_row = i;
        g1.setWidget( i, 0, new Label( "The password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox cleartextPassword2_box = new PasswordTextBox();
        cleartextPassword2_box.setWidth("180");
        g1.setWidget( i++, 1, cleartextPassword2_box );

        ClickListener RecoverButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 3; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( usernameOrEmail_box.getText().length() < 1 )
                {
                    Label l = new Label( "Username is empty!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( usernameOrEmail_row, 2, l);
                    formOk = false;
                }
				/* no spaces in username */
				if ( usernameOrEmail_box.getText().matches(".*[ \t]+.*") ) {
					Label l = new Label ("Username cannot have spaces, sorry!");
					l.setStyleName ("euca-error-hint");
					g1.setWidget (usernameOrEmail_row, 2, l);
					formOk = false;
				}

                if ( cleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password must be at least " + minPasswordLength + " characters long!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }
                if ( !cleartextPassword1_box.getText().equals( cleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password2_row, 2, l );
                    formOk = false;
                }
                if ( cleartextPassword1_box.getText().toLowerCase().matches(".*" +
                        usernameOrEmail_box.getText().toLowerCase() + ".*")) {
                    Label l = new Label ( "Password may not contain the username!");
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( password1_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");

                    UserInfoWeb user = new UserInfoWeb(
                            usernameOrEmail_box.getText(),
                            "", /* don't care about the name */
                            usernameOrEmail_box.getText(), /* same as login */
                            GWTUtils.md5(cleartextPassword1_box.getText()) );
                    EucalyptusWebBackend.App.getInstance().recoverPassword(
                            user,
                            new AsyncCallback() {
                                public void onSuccess( Object result )
                                {
                                    displayDialog( "Thank you!", ( String ) result );
                                }

                                public void onFailure( Throwable caught )
                                {
                                    String m = caught.getMessage();
                                	displayErrorPage(m);
                                }
                            }
                    );

                }
                else
                {
                    label_box.setText( "Please, fix the errors and resubmit:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button submit_button = new Button ( "Recover Password", RecoverButtonListener );
        Button cancel_button = new Button ( "Cancel", DefaultPageButtonListener );
        VerticalPanel mpanel = new VerticalPanel();
        mpanel.add( g1 );

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( submit_button );
        bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
        bpanel.add( cancel_button );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (mpanel);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

    private Button displayDialog ( String greeting, String message )
    {
		return displayDialog (greeting, message, null);
    }

 	private Button displayDialog ( String greeting, String message, Button firstButton )
    {
		if ( message==null || message.equalsIgnoreCase("") ) {
            message = "Server is not accessible!"; // TODO: any other reasons why message would be empty?
        }
        label_box.setText( greeting );
        label_box.setStyleName("euca-greeting-normal");
        Label m = new Label ( message );
        m.setWidth("300");

        VerticalPanel panel = new VerticalPanel();
        panel.add(m);
        panel.setStyleName("euca-login-panel");
        panel.setCellHorizontalAlignment(m, HasHorizontalAlignment.ALIGN_CENTER);
        panel.setCellVerticalAlignment(m, HasVerticalAlignment.ALIGN_MIDDLE);
        Button ok_button = new Button( "Ok", DefaultPageButtonListener );

		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.setSpacing (10);
		if (firstButton!=null) {
			hpanel.add (firstButton);
		}
		hpanel.add (ok_button);

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (panel);
        vpanel.add (hpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );

        return ok_button;
	}

    private boolean isPasswordExpired (UserInfoWeb user) {
        final long now = System.currentTimeMillis();
        if ((now > 0) && (now >= user.getPasswordExpires().longValue())) {
            return true;
        }
        return false;
    }

    public void attemptLogin()
    {
        displayStatusPage("Logging into the server...");
        EucalyptusWebBackend.App.getInstance().getUserRecord(
                sessionId,
                null, /* get user record associated with this sessionId */
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        loggedInUser = ( UserInfoWeb ) ( (List) result).get(0);
                        if ( currentAction == null )
                        {
                            if (isPasswordExpired(loggedInUser)) {
                                displayPasswordChangePage( true );
                            } else {
                                displayDefaultPage();
                            }
                        }
                        else
                        {
                            executeAction( currentAction );
                        }
                    }

                    public void onFailure( Throwable caught )
                    {
                        displayLoginErrorPage( caught.getMessage() );
                    }
                }
        );
    }

    public void attemptAction( String action, String param )
    {
        displayStatusPage( "Contacting the server..." );
        EucalyptusWebBackend.App.getInstance().performAction(
                sessionId,
                action,
                param,
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        displayMessagePage( ( String ) result );
                    }

                    public void onFailure( Throwable caught )
                    {
                        displayErrorPage( caught.getMessage() );
                    }
                }
        );
    }

    public void executeAction( String action )
    {
        /* NOTE: some of the checks are repeated by the server,
         * this is just to avoid unnecessary RPCs */
        if ( action.equals ( "approve" )
                || action.equals ( "reject" )
                || action.equals ( "delete" )
                || action.equals ( "disable" )
                || action.equals ( "enable" ) )
        {
            String userName = ( String ) urlParams.get( "user" );
            if ( !loggedInUser.isAdministrator().booleanValue() )
            {
                displayErrorPage( "Administrative privileges required" );
            }
            else if ( userName == null )
            {
                displayErrorPage( "Username not specified" );
            }
            else
            {
                attemptAction( action, userName );
            }
        }
        else if ( action.equals ( "delete_image")
                || action.equals ( "disable_image")
                || action.equals ( "enable_image") )
        {
            String imageId = ( String ) urlParams.get ("id");
            if ( !loggedInUser.isAdministrator().booleanValue() )
            {
                displayErrorPage( "Administrative privileges required" );
            }
            else if ( imageId == null )
            {
                displayErrorPage( "Image ID not specified" );
            }
            else
            {
                attemptAction( action, imageId );
            }
        }
        else if ( action.equals( "confirm" )
 			|| action.equals ("recover") )
        {
            String confirmationCode = ( String ) urlParams.get( "code" );
            if ( confirmationCode == null )
            {
                displayErrorPage( "Confirmation code not specified" );
            }
            else
            {
                attemptAction( action, confirmationCode );
            }
        }
        else
        {
            displayErrorPage( "Action '" + action + "' not recognized" );
        }
    }

    public void displayDefaultPage()
    {
		displayStatusPage("Loading default page...");

        /* If there is an action encoded in the URL, then redirect to
         * a URL without that action, so it is not repeated upon a reload
         * However, reserve the currently selected tab in URL.
         */
        if (currentAction!=null) {
            String extra = "";
            if (currentTabIndex!=0) {
                extra = "?page=" + currentTabIndex;
            }
            GWTUtils.redirect (GWT.getModuleBaseURL() + extra);
        }

        if (loggedInUser!=null) {
            if ( loggedInUser.isAdministrator().booleanValue() )
            {
                if (loggedInUser.getEmail().equalsIgnoreCase( "" ) ) {
                    displayAdminEmailChangePage();
                } else {
                    displayBarAndTabs();
                }
            }
            else
            {
                displayBarAndTabs();
            }
        } else {
            displayLoginPage();
        }
    }

    ClickListener AddUserButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            displayUserRecordPage (RootPanel.get(), null);
        }
    };

    ClickListener DefaultPageButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            displayDefaultPage();
        }
    };

    public void displayErrorPage( String message )
    {
        displayDialog("Error!", message);
        label_box.setStyleName("euca-greeting-error");
    }

    public void displayErrorPageFinal( String message )
    {
        Button ok_button = displayDialog("Error!", message);
        label_box.setStyleName("euca-greeting-error");
        ok_button.setVisible(false);
    }

    public void displayMessagePage( String message )
    {
        displayDialog("", message);
        label_box.setStyleName("euca-greeting-normal");
    }

    ClickListener LogoutButtonListener = new ClickListener() {
        public void onClick( Widget sender )
        {
            EucalyptusWebBackend.App.getInstance().logoutSession(
                    sessionId,
                    new AsyncCallback() {
                        public void onSuccess( Object result )
                        {
                            displayLoginPage();
                        }

                        public void onFailure( Throwable caught )
                        {
                            displayLoginPage();
                        }
                    }
            );
            sessionId = null; /* invalidate sessionId */
            loggedInUser = null; /* invalidate user */
            Cookies.removeCookie( cookie_name ); // TODO: this isn't working for some reason
        }
    };

    public void displayBarAndTabs()
    {
        /* top bar */
        HorizontalPanel top_bar = new HorizontalPanel();
        top_bar.setStyleName("euca-top-bar");
        top_bar.setSize("100%", "25");
        Label welcome = new Label( cloud_name);
        top_bar.add(welcome);
        top_bar.setCellHorizontalAlignment(welcome, HorizontalPanel.ALIGN_LEFT);
        top_bar.setCellVerticalAlignment(welcome, HorizontalPanel.ALIGN_MIDDLE);
        HorizontalPanel upanel = new HorizontalPanel();
        Label user_name = new HTML("Logged in as <b>"
                + loggedInUser.getUserName()
                + "</b>&nbsp;&nbsp;|&nbsp;&nbsp;");
        Hyperlink logout_button = new Hyperlink("Logout", "logout");
        logout_button.addClickListener(LogoutButtonListener);
        upanel.add(user_name);
        upanel.add(logout_button);
        top_bar.add(upanel);
        top_bar.setCellHorizontalAlignment(upanel, HorizontalPanel.ALIGN_RIGHT);
        top_bar.setCellVerticalAlignment(upanel, HorizontalPanel.ALIGN_MIDDLE);

        final VerticalPanel wrapper = new VerticalPanel();
        wrapper.setSize("100%", "80%");
        wrapper.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        wrapper.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);

		// set up tab layout so that *TabIndex variables are set in the beginning
		int nTabs = 0;
        allTabs = new TabBar();
        allTabs.addTab ("Credentials"); credsTabIndex = nTabs++;
        allTabs.addTab ("Images"); imgTabIndex = nTabs++;
        /////allTabs.addTab ("Instances"); instTabIndex = nTabs++;
        if (loggedInUser.isAdministrator().booleanValue()) {
			allTabs.addTab ("Users"); usrTabIndex = nTabs++;
			allTabs.addTab ("Configuration"); confTabIndex = nTabs++;
        }
		allTabs.addTabListener(new TabListener() {
            public void onTabSelected(SourcesTabEvents sender, int tabIndex) {
                String error = "This tab is not implemented yet, sorry!";
                wrapper.clear();
                currentTabIndex = tabIndex;
                if (tabIndex==credsTabIndex) { displayCredentialsTab(wrapper); }
                else if (tabIndex==imgTabIndex) { displayImagesTab(wrapper); }
                else if (tabIndex==usrTabIndex) { displayUsersTab(wrapper); }
				else if (tabIndex==confTabIndex) { displayConfTab(wrapper); }
                else { displayErrorPage("Invalid tab!"); }
            }
            public boolean onBeforeTabSelected(SourcesTabEvents sender, int tabIndex) {
                return true; /* here we could do checking for clicks on disabled tabs */
            }
        });
        allTabs.selectTab(currentTabIndex);
        RootPanel.get().clear();
        RootPanel.get().add( top_bar );
        RootPanel.get().add( allTabs );
        RootPanel.get().add( wrapper );
    }

    public void displayCredentialsTab (VerticalPanel parent)
    {
        History.newItem("credentials");

		VerticalPanel ppanel = new VerticalPanel();
		ppanel.setSpacing (5);
		ppanel.add (new HTML ("<h3>Account Information</h3>"));
		ppanel.add (new HTML ("<b>Login:</b> " + loggedInUser.getUserName()));
		ppanel.add (new HTML ("<b>Name:</b> " + loggedInUser.getRealName()));
		ppanel.add (new HTML ("<b>Email:</b> " + loggedInUser.getEmail()));
		ppanel.add (new HTML (user_account_text));
		ppanel.setStyleName( "euca-text" );
        Button passwordButton = new Button ( "Change Password",
                new ClickListener() {
                    public void onClick(Widget sender) {
                        displayPasswordChangePage (false);
                    }
                });
		Button editButton = new Button ( "Edit Account Information",
			new ClickListener() {
					public void onClick (Widget sender) {
						displayUserRecordPage (RootPanel.get(), loggedInUser);
					}
			});
        VerticalPanel ppanel2 = new VerticalPanel();
        ppanel2.setSpacing( 5 );
		ppanel2.add(editButton);
        ppanel2.add(passwordButton);

		VerticalPanel cpanel = new VerticalPanel();
		cpanel.add ( new HTML (certificate_download_text) );
        cpanel.setStyleName( "euca-text" );
		Button certButton = new Button ("Download Certificate",new ClickListener() {
				public void onClick (Widget sender) {
					Window.open(GWT.getModuleBaseURL() +
						"getX509?user=" + loggedInUser.getUserName() +
						"&keyValue=" + loggedInUser.getUserName() +
						"&code=" + loggedInUser.getCertificateCode(),
						"_self", "");
				}
		});

        VerticalPanel rpanel = new VerticalPanel();
		rpanel.setSpacing (5);
        rpanel.add( new HTML (rest_credentials_text) );
		final HTML secretStrings = new HTML
			("<p><b>Query ID:</b> <font color=#666666 size=\"1\">"
			+ loggedInUser.getQueryId() + "</font></br>"
            + "<b>Secret key:</b> <font color=#666666 size=\"1\">"
                + loggedInUser.getSecretKey() + "</font></p>");
		secretStrings.setVisible (false);
		rpanel.add (secretStrings);
        rpanel.setStyleName( "euca-text" );
		final Button secretButton = new Button ( "Show keys" );
		secretButton.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                if (secretStrings.isVisible()) {
					secretStrings.setVisible(false);
					secretButton.setText ("Show keys");
				} else {
					secretStrings.setVisible(true);
					secretButton.setText ("Hide keys");
				}
            }
        });

        final Grid g = new Grid( 3, 2 );
        g.getColumnFormatter().setWidth(0, "320");
        g.getColumnFormatter().setWidth(1, "200");
        g.setCellSpacing( 30 );

        g.setWidget( 0, 0, ppanel ); g.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
        g.setWidget( 0, 1, ppanel2); g.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);

        g.setWidget( 1, 0, cpanel ); g.getCellFormatter().setVerticalAlignment(1, 0, HasVerticalAlignment.ALIGN_TOP);
        g.setWidget( 1, 1, certButton ); g.getCellFormatter().setVerticalAlignment(1, 1, HasVerticalAlignment.ALIGN_TOP);

        g.setWidget( 2, 0, rpanel ); g.getCellFormatter().setVerticalAlignment(2, 0, HasVerticalAlignment.ALIGN_TOP);
		g.setWidget( 2, 1, secretButton ); g.getCellFormatter().setVerticalAlignment(2, 1, HasVerticalAlignment.ALIGN_TOP);

        parent.add(g);
    }

    public void displayErrorTab(VerticalPanel parent, String message)
    {
        parent.add(new Label(message));
    }

    public void displayTestingTab(VerticalPanel parent)
    {
        parent.add(new Label("truth = [" + props.get("truth") + "]"));
    }

    public void displayStatusPage(String message)
    {
        label_box.setText( message );
        label_box.setStyleName("euca-greeting-pending");

        final VerticalPanel wrapper = new VerticalPanel();
        wrapper.setSize("100%", "100%");
        wrapper.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        wrapper.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        wrapper.add(label_box);

        RootPanel.get().clear();
        RootPanel.get().add(wrapper);
    }

    public void displayPasswordChangePage(boolean mustChange)
    {
        if (mustChange) {
            label_box.setText( "You are required to change your password" );
            label_box.setStyleName("euca-greeting-error");
        } else {
            label_box.setText( "Please, change your password" );
            label_box.setStyleName("euca-greeting-normal");
        }
        final Grid g1 = new Grid ( 3, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        final int oldPassword_row = i;
        g1.setWidget( i, 0, new Label( "Old password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox oldPassword_box = new PasswordTextBox();
        oldPassword_box.setWidth("180");
        if (!mustChange) { /* don't ask for old password if the change is involuntary */
            g1.setWidget( i++, 1, oldPassword_box );
        }

        final int newPassword1_row = i;
        g1.setWidget( i, 0, new Label( "New password:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox newCleartextPassword1_box = new PasswordTextBox();
        newCleartextPassword1_box.setWidth("180");
        g1.setWidget( i++, 1, newCleartextPassword1_box );

        final int newPassword2_row = i;
        g1.setWidget( i, 0, new Label( "New password, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final PasswordTextBox newCleartextPassword2_box = new PasswordTextBox();
        newCleartextPassword2_box.setWidth("180");
        g1.setWidget( i++, 1, newCleartextPassword2_box );

        ClickListener ChangeButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 3; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( newCleartextPassword1_box.getText().length() < minPasswordLength )
                {
                    Label l = new Label( "Password is too short!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( newPassword1_row, 2, l );
                    formOk = false;
                }
                if ( !newCleartextPassword1_box.getText().equals( newCleartextPassword2_box.getText() ) )
                {
                    Label l = new Label( "Passwords do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( newPassword2_row, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");

                    EucalyptusWebBackend.App.getInstance().changePassword(
                            sessionId,
                            GWTUtils.md5(oldPassword_box.getText()),
                            GWTUtils.md5(newCleartextPassword1_box.getText()),
                            new AsyncCallback() {
                                public void onSuccess( final Object result )
                                {
                                    /* password change succeded - pull in the new user record */
                                    label_box.setText( "Refreshing user data..." );
                                    EucalyptusWebBackend.App.getInstance().getUserRecord(
                                            sessionId,
                                            null,
                                            new AsyncCallback() {
                                                public void onSuccess( Object result2 )
                                                {
                                                    loggedInUser = ( UserInfoWeb ) ( (List) result2).get(0);
                                                    displayMessagePage( ( String ) result );
                                                }
                                                public void onFailure( Throwable caught )
                                                {
                                                    displayLoginErrorPage( caught.getMessage() );
                                                }
                                            });
                                }
                                public void onFailure( Throwable caught )
                                {
                                    String m = caught.getMessage();
                                    label_box.setText( m );
                                    label_box.setStyleName("euca-greeting-warning");
                                }
                            }
                    );
                }
                else
                {
                    label_box.setText( "Please, fix the errors and try again:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button change_button = new Button( "Change password", ChangeButtonListener );
        Button cancel_button = new Button( "Cancel", DefaultPageButtonListener );

        HorizontalPanel bpanel = new HorizontalPanel();
        bpanel.add( change_button );
        if (!mustChange) {
            bpanel.add( new HTML( "&nbsp;&nbsp;or&nbsp;&nbsp;" ) );
            bpanel.add( cancel_button );
        }

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (g1);
        vpanel.add (bpanel);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }


    public void displayAdminEmailChangePage()
    {
        label_box.setText( "One more thing!" );
        label_box.setStyleName("euca-greeting-error");

        final Grid g1 = new Grid ( 2, 3 );
        g1.getColumnFormatter().setWidth(0, "180");
        g1.getColumnFormatter().setWidth(1, "180");
        g1.getColumnFormatter().setWidth(2, "180");
        int i = 0;

        g1.setWidget( i, 0, new Label( "Email address:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress1_box = new TextBox();
        emailAddress1_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress1_box );

        g1.setWidget( i, 0, new Label( "The address, again:" ) );
        g1.getCellFormatter().setHorizontalAlignment(i, 0, HasHorizontalAlignment.ALIGN_RIGHT);
        final TextBox emailAddress2_box = new TextBox();
        emailAddress2_box.setWidth("180");
        g1.setWidget( i++, 1, emailAddress2_box );

        ClickListener ChangeButtonListener = new ClickListener() {
            public void onClick( Widget sender )
            {
                boolean formOk = true;

                for ( int j = 0; j < 2; j++ )
                {
                    g1.clearCell( j, 2 ); /* clear previous right-hand-side annotations */
                }

                /* perform checks */
                if ( emailAddress1_box.getText().length() < 3 )
                {
                    Label l = new Label( "Invalid address!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( 0, 2, l );
                    formOk = false;
                }
                if ( !emailAddress1_box.getText().equals( emailAddress2_box.getText() ) )
                {
                    Label l = new Label( "Addresses do not match!" );
                    l.setStyleName("euca-error-hint");
                    g1.setWidget( 1, 2, l );
                    formOk = false;
                }

                if ( formOk )
                {
                    loggedInUser.setEmail( emailAddress1_box.getText() );
                    label_box.setText( "Checking with the server..." );
                    label_box.setStyleName("euca-greeting-pending");
                    EucalyptusWebBackend.App.getInstance().updateUserRecord(
                            sessionId,
                            loggedInUser,
                            new AsyncCallback() {
                                public void onSuccess( final Object result )
                                {
                                    displayWalrusURLChangePage ();
                                }
                                public void onFailure( Throwable caught )
                                {
                                    loggedInUser.setEmail( "" );
                                    displayLoginErrorPage( caught.getMessage() );
                                }
                            });
                }
                else
                {
                    label_box.setText( "Please, fix the errors and try again:" );
                    label_box.setStyleName("euca-greeting-warning");
                }
            }
        };

        Button change_button = new Button( "Change address", ChangeButtonListener );
        HTML message = new HTML (admin_email_change_text);
        message.setWidth( "460" );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (message);
        vpanel.add (g1);
        vpanel.add (change_button);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

        RootPanel.get().clear();
        RootPanel.get().add( wrapper );
    }

	public static SystemConfigWeb conf = new SystemConfigWeb ();
    public void displayWalrusURLChangePage()
    {
        label_box.setText( "One last thing!  Really!!!" );
        label_box.setStyleName("euca-greeting-error");

		HorizontalPanel hpanel = new HorizontalPanel();
		hpanel.add (new Label ("Walrus URL:"));
		final TextBox box = new TextBox ();
		box.setVisibleLength (55);
		hpanel.add (box);

		EucalyptusWebBackend.App.getInstance().getSystemConfig(sessionId,
			new AsyncCallback( ) {
				public void onSuccess ( final Object result ) {
					conf = (SystemConfigWeb) result;
					box.setText (conf.getStorageUrl());
				}
				public void onFailure ( Throwable caught ) { }
			}
		);

        Button change_button = new Button( "Confirm URL",
 			new ClickListener() {
	            public void onClick( Widget sender )
	            {
					conf.setStorageUrl(box.getText());
					EucalyptusWebBackend.App.getInstance().setSystemConfig(sessionId,
						conf,
						new AsyncCallback() {
							public void onSuccess ( final Object result ) {
								currentTabIndex = 3; // TODO: change this to confTabIndex
								displayDefaultPage ();
							}
							public void onFailure ( Throwable caught ) {
								displayErrorPage ("Failed to save the URL (check 'Configuration' tab).");
							}
						}
					);
				}
			}
		);
        HTML message = new HTML (admin_walrus_setup_text);
        message.setWidth( "460" );

        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        if (logo!=null) { vpanel.add (logo); }
        vpanel.add (label_box);
        vpanel.add (message);
        vpanel.add (hpanel);
        vpanel.add (change_button);

        VerticalPanel wrapper = new VerticalPanel();
        wrapper.add (vpanel);
        wrapper.setSize("100%", "100%");
        wrapper.setCellHorizontalAlignment(vpanel, VerticalPanel.ALIGN_CENTER);
        wrapper.setCellVerticalAlignment(vpanel, VerticalPanel.ALIGN_MIDDLE);

		RootPanel.get().clear();
		RootPanel.get().add (wrapper);
	}

    public void displayUsersTab(final VerticalPanel parent)
    {
        History.newItem("users");
        final HTML msg = new HTML("Contacting the server...");
        EucalyptusWebBackend.App.getInstance().getUserRecord(
                sessionId,
                "*", /* we want all user records */
                new AsyncCallback() {
                    public void onSuccess( Object result )
                    {
                        List usersList = (List)result;
                        displayUsersList (usersList, parent);
                    }
                    public void onFailure( Throwable caught )
                    {
                        displayErrorPage (caught.getMessage());
                    }
                });

        parent.add(msg);
    }

    public void displayImagesTab (final VerticalPanel parent)
    {
        History.newItem("images");
        final HTML msg = new HTML ("Contacting the server...");
        EucalyptusWebBackend.App.getInstance().getImageInfo(
                sessionId,
                loggedInUser.getUserName(),
                new AsyncCallback() {
                    public void onSuccess (Object result)
                    {
                        List imagesList = (List)result;
                        displayImagesList (imagesList, parent);
                    }
                    public void onFailure (Throwable caught)
                    {
                        displayErrorPage (caught.getMessage());
                    }
                });

        parent.add(msg);
    }

	public void displayConfirmDeletePage( final String userName )
    {
		Button deleteButton = new Button ("Delete", new ClickListener() {
            public void onClick(Widget sender) {
				GWTUtils.redirect (GWT.getModuleBaseURL()
					+ "?action=delete"
					+ "&user=" + userName
					+ "&page=" + currentTabIndex);
			}
        });
		Button okButton = displayDialog ("Sure?",
			"Do you want to delete user '" + userName + "'?", deleteButton);
		okButton.setText ("Cancel");
		label_box.setStyleName("euca-greeting-warning");
    }

    private HTML userActionButton (String action, UserInfoWeb user)
    {
        return new HTML ("<a class=\"euca-action-link\" href=\"" + GWT.getModuleBaseURL()
                + "?action=" + action.toLowerCase()
                + "&user=" + user.getUserName()
                + "&page=" + currentTabIndex
                + "\">" + action + "</a>");
    }

	class EditCallback implements ClickListener {

		private EucalyptusWebInterface parent;
		private UserInfoWeb u;

		EditCallback ( final EucalyptusWebInterface parent, UserInfoWeb u )
		{
			this.parent = parent;
			this.u = u;
		}

		public void onClick( final Widget widget )
		{
			displayUserRecordPage (RootPanel.get(), u);
		}
	}

    public void displayUsersList(List usersList, final VerticalPanel parent)
    {
        parent.clear();
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(5);
        parent.add(vpanel);

        int nusers = usersList.size();
        if (nusers>0) {
            final Grid g = new Grid(nusers + 1, 6);
            g.setStyleName("euca-table");
            g.setCellPadding(6);
            g.setWidget(0, 0, new Label("Username"));
            g.setWidget(0, 1, new Label("Email"));
            g.setWidget(0, 2, new Label("Name"));
            g.setWidget(0, 3, new Label("Status"));
            g.setWidget(0, 4, new Label("Actions"));
            //g.setWidget(0, 5, new Label("View"));
            g.getRowFormatter().setStyleName(0, "euca-table-heading-row");

            for (int i=0; i<nusers; i++) {
                final UserInfoWeb u = (UserInfoWeb) usersList.get(i);
                int row = i+1;
                if ((row%2)==1) {
                    g.getRowFormatter().setStyleName(row, "euca-table-odd-row");
                } else {
                    g.getRowFormatter().setStyleName(row, "euca-table-even-row");
                }
				Label userLabel = new Label(u.getUserName());
                g.setWidget(row, 0, userLabel);
				Label emailLabel = new Label(u.getEmail());
                g.setWidget(row, 1, emailLabel);
				Label nameLabel = new Label(u.getRealName());
                g.setWidget(row, 2, nameLabel);
                String status;
                if (!u.isApproved().booleanValue()) {
                    status = "unapproved";
                } else if (!u.isEnabled().booleanValue()) {
                    status = "disabled";
                } else if (!u.isConfirmed().booleanValue()) {
                    status = "unconfirmed";
                } else {
                    status = "active";
                }
                if (u.isAdministrator().booleanValue()) {
                     status += " & admin";
                }
                g.setWidget(row, 3, new Label(status) );

                /* actions */
                HorizontalPanel ops = new HorizontalPanel();
                ops.setSpacing (3);
                HTML act_button = userActionButton ("Disable", u);
                if (!u.isApproved().booleanValue()) {
                    act_button = userActionButton ("Approve", u);
                } else if (!u.isEnabled().booleanValue()) {
                    act_button = userActionButton ("Enable", u);
                }
                ops.add(act_button);

				Label editLabel = new Label ("Edit");
				editLabel.addClickListener (new EditCallback(this, u));
				editLabel.setStyleName ("euca-action-link");
				ops.add(editLabel);

                //HTML del_button = userActionButton ("Delete", u);
		        Hyperlink del_button = new Hyperlink( "Delete", "confirmdelete" );
				del_button.setStyleName ("euca-action-link");
		        del_button.addClickListener( new ClickListener() {
                    public void onClick(Widget sender) {
                        displayConfirmDeletePage (u.getUserName());
                    }
                });
                ops.add(del_button);
                g.setWidget(row, 4, ops );

                /* view */
                HorizontalPanel views = new HorizontalPanel();
                views.setSpacing (3);
                HTML inst_button = userActionButton ("Instances", u);
                views.add(inst_button);
                HTML img_button = userActionButton ("Images", u);
                views.add(img_button);
                //g.setWidget(row, 5, views); TODO: implement 'views'
            }
            vpanel.add(g);
        } else {
            vpanel.add(new Label("No users found"));
        }
        vpanel.add(new Button ("Add user", AddUserButtonListener));
    }

    private HTML imageActionButton (String action, ImageInfoWeb img)
    {
        return new HTML ("<a class=\"euca-action-link\" href=\"" + GWT.getModuleBaseURL()
                + "?action=" + action.toLowerCase() + "_image"
                + "&id=" + img.getImageId()
                + "&page=" + currentTabIndex
                + "\">" + action + "</a>");
    }

    public void displayImagesList(List imagesList, final VerticalPanel parent)
    {
        parent.clear();
        int nimages = imagesList.size();
        boolean showActions = false;
        if (loggedInUser.isAdministrator().booleanValue()) {
            // Chris: comment out the next line if image deletion does not work on the back-end
            showActions = true;
        }

        if (nimages>0) {
            final Grid g = new Grid(nimages + 1, 6);
            g.setStyleName("euca-table");
            g.setCellPadding(6);
            g.setWidget(0, 0, new Label("Id"));
            g.setWidget(0, 1, new Label("Name"));
            g.setWidget(0, 2, new Label("Kernel"));
            g.setWidget(0, 3, new Label("Ramdisk"));
            g.setWidget(0, 4, new Label("State"));
            if ( showActions )
                g.setWidget(0, 5, new Label("Actions"));
            g.getRowFormatter().setStyleName(0, "euca-table-heading-row");

            for (int i=0; i<nimages; i++) {
                ImageInfoWeb img = (ImageInfoWeb) imagesList.get(i);
                int row = i+1;
                if ((row%2)==1) {
                    g.getRowFormatter().setStyleName(row, "euca-table-odd-row");
                } else {
                    g.getRowFormatter().setStyleName(row, "euca-table-even-row");
                }
                g.setWidget(row, 0, new Label (img.getImageId()) );
                g.setWidget(row, 1, new Label (img.getImageLocation()) );
                g.setWidget(row, 2, new Label (img.getKernelId()) );
                g.setWidget(row, 3, new Label (img.getRamdiskId()) );
                g.setWidget(row, 4, new Label (img.getImageState()));
                if ( showActions ) {
                    HorizontalPanel ops = new HorizontalPanel();
                    ops.setSpacing (3);
                    HTML act_button = imageActionButton ("Disable", img);
                    if (img.getImageState().equalsIgnoreCase("deregistered")) {
                        act_button = imageActionButton ("Enable", img);
                    }
                    ops.add(act_button);
                    // TODO: uncomment when deletion is implemented
                    //HTML del_button = imageActionButton ("Delete", img);
                    //ops.add(del_button);
                    g.setWidget(row, 5, ops );
                }
            }
            parent.add(g);
        } else {
            parent.add(new Label("No images found"));
        }
    }

    public void displayConfTab (final VerticalPanel parent)
    {
		History.newItem("conf");
        VerticalPanel vpanel = new VerticalPanel();
        vpanel.setSpacing(15);
        vpanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
		vpanel.add (new SystemConfigTable (sessionId));
		vpanel.add (new ClusterInfoTable (sessionId));
		vpanel.add (new VmTypeTable (sessionId));

		parent.clear();
		parent.add (vpanel);
	}
}
