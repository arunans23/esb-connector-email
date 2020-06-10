/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.connector.operations;

import org.apache.synapse.MessageContext;
import org.wso2.carbon.connector.connection.EmailConnectionManager;
import org.wso2.carbon.connector.connection.EmailConnectionPool;
import org.wso2.carbon.connector.connection.MailBoxConnection;
import org.wso2.carbon.connector.core.AbstractConnector;
import org.wso2.carbon.connector.exception.ContentBuilderException;
import org.wso2.carbon.connector.exception.EmailConnectionException;
import org.wso2.carbon.connector.exception.EmailConnectionPoolException;
import org.wso2.carbon.connector.exception.EmailNotFoundException;
import org.wso2.carbon.connector.exception.InvalidConfigurationException;
import org.wso2.carbon.connector.utils.EmailConstants;
import org.wso2.carbon.connector.utils.EmailUtils;
import org.wso2.carbon.connector.utils.Error;

import javax.mail.Flags;

import static java.lang.String.format;

/**
 * Marks an email as read
 */
public class EmailMarkAsDeleted extends AbstractConnector {

    @Override
    public void connect(MessageContext messageContext) {

        String folder = (String) getParameter(messageContext, EmailConstants.FOLDER);
        String emailID = (String) getParameter(messageContext, EmailConstants.EMAIL_ID);
        String errorString = "Error occurred while marking email with ID: %s as deleted.";
        EmailConnectionPool pool = null;
        MailBoxConnection connection = null;
        try {
            String connectionName = EmailUtils.getConnectionName(messageContext);
            pool = EmailConnectionManager.getEmailConnectionManager().getConnectionPool(connectionName);
            connection = (MailBoxConnection) pool.borrowObject();
            boolean status = EmailUtils.changeEmailState(connection, folder, emailID, Flags.Flag.DELETED,
                    false);
            EmailUtils.generateOutput(messageContext, status);
        } catch (EmailConnectionException | EmailConnectionPoolException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.CONNECTIVITY);
            handleException(format(errorString, emailID), e, messageContext);
        } catch (EmailNotFoundException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.EMAIL_NOT_FOUND);
            handleException(format(errorString, emailID), e, messageContext);
        } catch (InvalidConfigurationException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.INVALID_CONFIGURATION);
            handleException(format(errorString, folder), e, messageContext);
        } catch (ContentBuilderException e) {
            EmailUtils.setErrorsInMessage(messageContext, Error.RESPONSE_GENERATION);
            handleException(format(errorString, folder), e, messageContext);
        } finally {
            if (pool != null) {
                pool.returnObject(connection);
            }
        }

    }
}
