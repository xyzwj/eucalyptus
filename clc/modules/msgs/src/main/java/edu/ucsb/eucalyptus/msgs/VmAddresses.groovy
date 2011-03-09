/*******************************************************************************
*Copyright (c) 2009  Eucalyptus Systems, Inc.
* 
*  This program is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, only version 3 of the License.
* 
* 
*  This file is distributed in the hope that it will be useful, but WITHOUT
*  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
*  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
*  for more details.
* 
*  You should have received a copy of the GNU General Public License along
*  with this program.  If not, see <http://www.gnu.org/licenses/>.
* 
*  Please contact Eucalyptus Systems, Inc., 130 Castilian
*  Dr., Goleta, CA 93101 USA or visit <http://www.eucalyptus.com/licenses/>
*  if you need additional information or have any questions.
* 
*  This file may incorporate work covered under the following copyright and
*  permission notice:
* 
*    Software License Agreement (BSD License)
* 
*    Copyright (c) 2008, Regents of the University of California
*    All rights reserved.
* 
*    Redistribution and use of this software in source and binary forms, with
*    or without modification, are permitted provided that the following
*    conditions are met:
* 
*      Redistributions of source code must retain the above copyright notice,
*      this list of conditions and the following disclaimer.
* 
*      Redistributions in binary form must reproduce the above copyright
*      notice, this list of conditions and the following disclaimer in the
*      documentation and/or other materials provided with the distribution.
* 
*    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
*    IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
*    TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
*    PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
*    OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
*    EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
*    PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
*    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
*    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
*    NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
*    SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. USERS OF
*    THIS SOFTWARE ACKNOWLEDGE THE POSSIBLE PRESENCE OF OTHER OPEN SOURCE
*    LICENSED MATERIAL, COPYRIGHTED MATERIAL OR PATENTED MATERIAL IN THIS
*    SOFTWARE, AND IF ANY SUCH MATERIAL IS DISCOVERED THE PARTY DISCOVERING
*    IT MAY INFORM DR. RICH WOLSKI AT THE UNIVERSITY OF CALIFORNIA, SANTA
*    BARBARA WHO WILL THEN ASCERTAIN THE MOST APPROPRIATE REMEDY, WHICH IN
*    THE REGENTS' DISCRETION MAY INCLUDE, WITHOUT LIMITATION, REPLACEMENT
*    OF THE CODE SO IDENTIFIED, LICENSING OF THE CODE SO IDENTIFIED, OR
*    WITHDRAWAL OF THE CODE CAPABILITY TO THE EXTENT NEEDED TO COMPLY WITH
*    ANY SUCH LICENSES OR RIGHTS.
*******************************************************************************/
/*
 * Author: chris grzegorczyk <grze@eucalyptus.com>
 */
package edu.ucsb.eucalyptus.msgs

import com.eucalyptus.auth.policy.PolicyAction;
import com.eucalyptus.auth.policy.PolicySpec;
import com.eucalyptus.binding.HttpParameterMapping;
import edu.ucsb.eucalyptus.msgs.EucalyptusData;
import edu.ucsb.eucalyptus.msgs.EucalyptusMessage;

public class VmAddressMessage extends EucalyptusMessage{}
/** *******************************************************************************/
@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_ALLOCATEADDRESS )
public class AllocateAddressType extends VmAddressMessage {} //** added 2008-02-01  **/
public class AllocateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
}
/** *******************************************************************************/
@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_RELEASEADDRESS )
public class ReleaseAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;

  def ReleaseAddressType(final publicIp) {
    this.publicIp = publicIp;
  }

  def ReleaseAddressType() {}
}
public class ReleaseAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/
@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_DESCRIBEADDRESSES )
public class DescribeAddressesType extends VmAddressMessage { //** added 2008-02-01  **/
  @HttpParameterMapping (parameter = "PublicIp")
  ArrayList<String> publicIpsSet = new ArrayList<String>();
}
public class DescribeAddressesResponseType extends VmAddressMessage { //** added 2008-02-01  **/
  ArrayList<AddressInfoType> addressesSet = new ArrayList<AddressInfoType>();
}
/** *******************************************************************************/
@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_ASSOCIATEADDRESS )
public class AssociateAddressType extends VmAddressMessage { //** added 2008-02-01  **/
  String publicIp;
  String instanceId;

  def AssociateAddressType(final publicIp, final instanceId) {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }

  def AssociateAddressType() {
  }
}
public class AssociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/
@PolicyAction( vendor = PolicySpec.VENDOR_EC2, action = PolicySpec.EC2_DISASSOCIATEADDRESS )
public class DisassociateAddressType extends VmAddressMessage {  //** added 2008-02-01  **/
  String publicIp;
}
public class DisassociateAddressResponseType extends VmAddressMessage { //** added 2008-02-01  **/
}
/** *******************************************************************************/
public class AddressInfoType extends EucalyptusData {  //** added 2008-02-01  **/
  String publicIp;
  String instanceId;

  def AddressInfoType(final publicIp, final instanceId)
  {
    this.publicIp = publicIp;
    this.instanceId = instanceId;
  }

  def AddressInfoType()
  {
  }

}


