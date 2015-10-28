package au.org.ala.ecodata

import au.org.ala.web.AuthService
import net.sf.json.JSON
import net.sf.json.groovy.JsonSlurper

class UserService {

    static transactional = false
    AuthService authService
    WebService webService
    def grailsApplication

    private static ThreadLocal<UserDetails> _currentUser = new ThreadLocal<UserDetails>()

    def getCurrentUserDisplayName() {
        UserDetails currentUser = _currentUser.get()
        return currentUser ? currentUser.displayName : ""
    }

    def getCurrentUserDetails() {
        return _currentUser.get();
    }

    def lookupUserDetails(String userId) {

        def userDetails = getUserForUserId(userId)
        if (!userDetails) {
            log.warn("Unable to lookup user details for userId: ${userId}")
            userDetails = new UserDetails(userId: userId?:'<not recorded>', userName: 'unknown', displayName: 'Unknown')
        }

        userDetails
    }

    /**
     * Gets the CAS roles for the specified user. If no id is provided, then the currently authenticated user will be used
     *
     * @param userId The ID of the user whose roles you want to retrieve. Optional - if not provided, will return the roles for the currently authenticated user (if there is one)
     * @return List of {@link au.org.ala.web.CASRoles} names
     */
    List<String> getRolesForUser(String userId = null) {
        userId = userId ?: getCurrentUserDetails().userId

        List<String> roles = []
        if (userId) {
            Map userDetails = webService.doPost("${grailsApplication.config.userDetails.url}${grailsApplication.config.userDetailsById.path}?userName=${URLEncoder.encode(userId, 'UTF-8')}&includeProps=false", [:])?.resp

            roles.addAll(userDetails?.roles ?: [])
        }

        roles
    }

    synchronized def getUserForUserId(String userId) {
        if (!userId) {
            return null
        }
        return authService.getUserForUserId(userId)
    }

    /**
     * This method gets called by a filter at the beginning of the request (if a userId parameter is on the URL)
     * It sets the user details in a thread local for extraction by the audit service.
     * @param userId
     */
    def setCurrentUser(String userId) {

        def userDetails = lookupUserDetails(userId)
        if (userDetails) {
            _currentUser.set(userDetails)
            return userDetails
        } else {
            log.warn("Failed to lookup user details for user id ${userId}! No details set on thread local.")
        }
    }

    def clearCurrentUser() {
        if (_currentUser) {
            _currentUser.remove()
        }
    }
}
