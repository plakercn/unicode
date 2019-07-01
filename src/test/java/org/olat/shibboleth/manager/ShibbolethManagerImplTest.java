/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.shibboleth.manager;

import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.OrganisationRoles;
import org.olat.basesecurity.OrganisationService;
import org.olat.basesecurity.SecurityGroup;
import org.olat.basesecurity.manager.SecurityGroupDAO;
import org.olat.core.id.Identity;
import org.olat.core.id.Preferences;
import org.olat.core.id.User;
import org.olat.resource.accesscontrol.AccessControlModule;
import org.olat.resource.accesscontrol.provider.auto.AdvanceOrder;
import org.olat.resource.accesscontrol.provider.auto.AutoAccessManager;
import org.olat.shibboleth.ShibbolethDispatcher;
import org.olat.user.UserManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 *
 * Initial date: 19.07.2017<br>
 * @author uhensler, urs.hensler@frentix.com, http://www.frentix.com
 *
 */
public class ShibbolethManagerImplTest {

	@Mock
	private AccessControlModule acModuleMock;
	@Mock
	private BaseSecurity securityManagerMock;
	@Mock
	private SecurityGroupDAO securityGroupDaoMock;
	@Mock
	private SecurityGroup securityGroupOlatusersMock;
	@Mock
	private SecurityGroup securityGroupAuthorMock;
	@Mock
	private UserManager userManagerMock;
	@Mock
	private AutoAccessManager autoAccessManagerMock;
	@Mock
	private OrganisationService organisationServiceMock;
	@Mock
	private ShibbolethAdvanceOrderInput advanceOrderInputMock;
	@Mock
	private Identity identityMock;
	@Mock
	private User userMock;
	@Mock
	private Preferences preferencesMock;
	@Mock
	private ShibbolethAttributes attributesMock;

	private TestableShibbolethManagerImpl sut = new TestableShibbolethManagerImpl();

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(sut, "securityManager", securityManagerMock);
		ReflectionTestUtils.setField(sut, "acModule", acModuleMock);
		ReflectionTestUtils.setField(sut, "autoAccessManager", autoAccessManagerMock);
		ReflectionTestUtils.setField(sut, "userManager", userManagerMock);
		ReflectionTestUtils.setField(sut, "organisationService", organisationServiceMock);

		when(securityManagerMock.createAndPersistIdentityAndUser(anyString(), isNull(), any(User.class), anyString(), anyString()))
				.thenReturn(identityMock);
		when(userManagerMock.createUser(null, null, null)).thenReturn(userMock);
		when(identityMock.getUser()).thenReturn(userMock);
		when(userMock.getPreferences()).thenReturn(preferencesMock);
		when(attributesMock.syncUser(any(User.class))).then(returnsFirstArg());
		when(attributesMock.getAcRawValues()).thenReturn("values");
	}

	@Test
	public void shouldCreateAndPersistNewUser() {
		sut.createUser(anyString(), anyString(), anyString(), attributesMock);

		verify(securityManagerMock).createAndPersistIdentityAndUser(
				anyString(), eq(null), eq(userMock), eq(ShibbolethDispatcher.PROVIDER_SHIB), anyString());
	}

	@Test
	public void shouldAddNewUserToUsersGroup() {
		sut.createUser(anyString(), anyString(), anyString(), attributesMock);

		verify(organisationServiceMock).addMember(identityMock, OrganisationRoles.user);
	}

	@Test
	public void shouldAddUserToAuthorGroupIfIsAuthorWhenCreating() {
		when(attributesMock.isAuthor()).thenReturn(true);

		sut.createUser(anyString(), anyString(), anyString(), attributesMock);

		verify(organisationServiceMock).addMember(identityMock, OrganisationRoles.author);
	}

	@Test
	public void shouldAddUserToAuthorGroupIfIsAuthorWhenSyncing() {
		when(attributesMock.isAuthor()).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(organisationServiceMock).addMember(identityMock, OrganisationRoles.author);
	}

	@Test
	public void shouldNotRemoveFromAuthorGroupIfIsNotAuthor() {
		when(attributesMock.isAuthor()).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(securityGroupDaoMock, never()).removeIdentityFromSecurityGroup(identityMock, securityGroupAuthorMock);
	}

	@Test
	public void shouldCreateAdvanceOrderWhenCreating() {
		when(acModuleMock.isAutoEnabled()).thenReturn(true);

		sut.createUser(anyString(), anyString(), anyString(), attributesMock);

		verify(autoAccessManagerMock).createAdvanceOrders(advanceOrderInputMock);
	}

	@Test
	public void shouldBookAdvanceOrderWhenCreating() {
		when(acModuleMock.isAutoEnabled()).thenReturn(true);

		sut.createUser(anyString(), anyString(), anyString(), attributesMock);

		verify(autoAccessManagerMock).grantAccessToCourse(identityMock);
	}

	@Test
	public void shouldCreateAdvanceOrderWhenSyncing() {
		when(acModuleMock.isAutoEnabled()).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(autoAccessManagerMock).createAdvanceOrders(advanceOrderInputMock);
	}

	@Test
	public void shouldBookAdvanceOrderWhenSyncing() {
		when(acModuleMock.isAutoEnabled()).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(autoAccessManagerMock).grantAccessToCourse(identityMock);
	}


	@Test
	public void shouldNotCreateAdvanceOrderWhenDisabled() {
		when(acModuleMock.isAutoEnabled()).thenReturn(false);

		sut.syncUser(identityMock, attributesMock);

		verify(autoAccessManagerMock, never()).createAdvanceOrders(null);
	}

	@Test
	public void shouldNotBookAdvanceOrderWhenDisabled() {
		when(acModuleMock.isAutoEnabled()).thenReturn(false);
		Collection<AdvanceOrder> advanceOrders = new HashSet<>();
		when(autoAccessManagerMock.loadPendingAdvanceOrders(identityMock)).thenReturn(advanceOrders);

		sut.syncUser(identityMock, attributesMock);

		verify(autoAccessManagerMock, never()).grantAccess(advanceOrders);
	}

	@Test
	public void shouldSyncUserWhenAttributesChanged() {
		when(attributesMock.hasDifference(userMock)).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(attributesMock).syncUser(userMock);
	}

	@Test
	public void shouldUpdateWhenUserAttributesChanged() {
		when(attributesMock.hasDifference(userMock)).thenReturn(true);

		sut.syncUser(identityMock, attributesMock);

		verify(userManagerMock).updateUser(userMock);
	}

	@Test
	public void shouldNotUpdateUserWhenAttributesNotChanged() {
		when(attributesMock.hasDifference(userMock)).thenReturn(false);

		sut.syncUser(identityMock, attributesMock);

		verify(userManagerMock, never()).updateUser(userMock);
	}

	private class TestableShibbolethManagerImpl extends ShibbolethManagerImpl {

		@Override
		protected ShibbolethAdvanceOrderInput getShibbolethAdvanceOrderInput() {
			return advanceOrderInputMock;
		}

	}
}