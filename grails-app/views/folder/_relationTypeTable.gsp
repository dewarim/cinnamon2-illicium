<%@ page import="server.RelationResolver" %>
<table>
	<tbody>

	<tr class="prop">
		<td valign="top" class="name"><g:message code="relationType.id"/></td>

		<td valign="top" class="value">${fieldValue(bean: relationType, field: 'id')}</td>

	</tr>

	<tr class="prop">
		<td valign="top" class="name"><g:message code="relationType.name"/></td>

		<td valign="top" class="value">${fieldValue(bean: relationType, field: 'name')}</td>

	</tr>

	<tr class="prop">
		<td valign="top" class="name"><g:message code="relationType.description"/></td>

		<td valign="top" class="value">${fieldValue(bean: relationType, field: 'description')}</td>

	</tr>

	<tr class="prop">
		<td valign="top" class="name"><g:message code="relationType.leftobjectprotected"/></td>

		<td valign="top" class="value">
			<g:if test="${relationType.leftobjectprotected}">
				<r:img uri="/images/ok.png" plugin='humulus' alt="${message(code: "input.disabled")}"/>
			</g:if>
			<g:else>
                <r:img uri="/images/no.png" plugin='humulus' alt="${message(code: "input.enabled")}"/>
			</g:else>
		</td>

	</tr>

	<tr class="prop">
		<td valign="top" class="name"><g:message code="relationType.rightobjectprotected"/></td>

		<td valign="top" class="value">
			<g:if test="${relationType.rightobjectprotected}">
                <r:img uri="/images/ok.png" plugin='humulus' alt="${message(code: "input.disabled")}"/>
            </g:if>
            <g:else>
                <r:img uri="/images/no.png" plugin='humulus' alt="${message(code: "input.enabled")}"/>
			</g:else>
		</td>
	</tr>
		<tr class="prop">
			<td>
				<g:message code="relationType.leftResolver"/>
			</td>
					<td valign="top" class="value">
						${relationType.leftResolver.name}
					</td>
				</tr>
				<tr class="prop">
					<td>
						<g:message code="relationType.rightResolver"/>
					</td>
					<td valign="top" class="value">
						${relationType.rightResolver.name}
					</td>
				</tr>


	</tbody>
</table>