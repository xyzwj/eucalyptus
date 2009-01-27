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
 * Author: Sunil Soman sunils@cs.ucsb.edu
 */

package edu.ucsb.eucalyptus.ic;

import edu.ucsb.eucalyptus.cloud.EucalyptusCloudException;
import edu.ucsb.eucalyptus.msgs.DescribeAvailabilityZonesType;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;
import edu.ucsb.eucalyptus.util.EucalyptusProperties;
import org.apache.log4j.Logger;
import org.mule.api.MuleException;
import org.mule.module.client.MuleClient;

public class WalrusMessaging {

    private static Logger LOG = Logger.getLogger( WalrusMessaging.class );
    private static MuleClient client = null;

    private static MuleClient getClient() throws MuleException
    {
        synchronized ( WalrusMessaging.class )
        {
            if ( client == null )
                client = new MuleClient();
        }
        return client;
    }

    private static boolean first = true;
    public static void enqueue( EucalyptusMessage msg ) throws EucalyptusCloudException
    {
        try
        {
            if( first )
            {
                first = false;
                DescribeAvailabilityZonesType descAZMsg = new DescribeAvailabilityZonesType();
                descAZMsg.setUserId( EucalyptusProperties.NAME );
                descAZMsg.setEffectiveUserId( EucalyptusProperties.NAME );
                getClient().dispatch( "vm://Request", descAZMsg, null );
            }
            getClient().dispatch( "vm://WalrusRequestQueue", msg, null );
        }
        catch ( MuleException e )
        {
            LOG.error( e );
            throw new EucalyptusCloudException( e );
        }
    }

    public static EucalyptusMessage dequeue( String msgId )
    {
        return WalrusReplyQueue.getReply( msgId );
    }


}