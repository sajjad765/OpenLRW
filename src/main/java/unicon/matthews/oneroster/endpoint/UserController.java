package unicon.matthews.oneroster.endpoint;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import unicon.matthews.caliper.Event;
import unicon.matthews.caliper.exception.EventNotFoundException;
import unicon.matthews.caliper.service.EventService;
import unicon.matthews.common.exception.BadRequestException;
import unicon.matthews.entity.MongoUserMappingRepository;
import unicon.matthews.entity.UserMapping;
import unicon.matthews.oneroster.Enrollment;
import unicon.matthews.oneroster.Result;
import unicon.matthews.oneroster.User;
import unicon.matthews.oneroster.exception.EnrollmentNotFoundException;
import unicon.matthews.oneroster.exception.ExceptionResponse;
import unicon.matthews.oneroster.exception.ResultNotFoundException;
import unicon.matthews.oneroster.exception.UserNotFoundException;
import unicon.matthews.oneroster.service.EnrollmentService;
import unicon.matthews.oneroster.service.ResultService;
import unicon.matthews.oneroster.service.UserService;
import unicon.matthews.oneroster.service.repository.MongoUser;
import unicon.matthews.security.auth.JwtAuthenticationToken;
import unicon.matthews.security.model.UserContext;

/**
 * @author ggilbert
 * @author xchopin <xavier.chopin@univ-lorraine.fr>
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private UserService userService;
  private EnrollmentService enrollmentService;
  private MongoUserMappingRepository mongoUserMappingRepository;
  private ResultService resultService;
  private EventService eventService;

  @Autowired
  public UserController(UserService userService, EnrollmentService enrollmentService, MongoUserMappingRepository mongoUserMappingRepository, ResultService resultService, EventService eventService) {
    this.userService = userService;
    this.enrollmentService = enrollmentService;
    this.mongoUserMappingRepository = mongoUserMappingRepository;
    this.resultService = resultService;
    this.eventService = eventService;
  }

  /**
   * POST /api/users
   *
   * Inserts a user into the DBMS (MongoDB).
   * @param token  JWT
   * @param user   user to insert
   * @param check  boolean to know if it has to check duplicates (takes more time)
   * @return       HTTP Response
   */
  @RequestMapping(method = RequestMethod.POST)
  public ResponseEntity<?> post(JwtAuthenticationToken token, @RequestBody User user, @RequestParam(value="check", required=false) Boolean check) {
    UserContext userContext = (UserContext) token.getPrincipal();
    User savedUser = this.userService.save(userContext.getTenantId(), userContext.getOrgId(), user, (check == null) ? true : check);
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.setLocation(ServletUriComponentsBuilder
            .fromCurrentRequest().path("/{id}")
            .buildAndExpand(savedUser.getSourcedId()).toUri());

    return new ResponseEntity<>(savedUser, httpHeaders, HttpStatus.CREATED);
  }

  /**
   * GET /api/users
   *
   * Returns all the users for a tenant id and an organization id given.
   * @param token                 a JWT to get authenticated
   * @return                      the users
   * @throws UserNotFoundException
   */
  @RequestMapping(method = RequestMethod.GET)
  public Collection<MongoUser> getUsers(JwtAuthenticationToken token) throws UserNotFoundException {
    UserContext userContext = (UserContext) token.getPrincipal();
    return userService.findAll(userContext.getTenantId(), userContext.getOrgId());
  }

  @RequestMapping(value = "/{userId:.+}", method = RequestMethod.GET)
  public User getUser(JwtAuthenticationToken token, @PathVariable("userId") final String userId) throws UserNotFoundException {
    UserContext userContext = (UserContext) token.getPrincipal();
    return userService.findBySourcedId(userContext.getTenantId(), userContext.getOrgId(), userId);
  }

  /**
   * DELETE /api/users/:id
   *
   * Deletes a user for its id given.
   * @param token   a JWT to get authenticated
   * @param userId  id of the aimed user
   * @return        HTTP Response (200 or 404)
   * @throws UserNotFoundException
   */
  @RequestMapping(value = "/{userId:.+}", method = RequestMethod.DELETE)
  public ResponseEntity deleteUser(JwtAuthenticationToken token, @PathVariable("userId") final String userId) throws UserNotFoundException {
    UserContext userContext = (UserContext) token.getPrincipal();
    return userService.delete(userContext.getTenantId(), userContext.getOrgId(), userId) ?
            new ResponseEntity<>(HttpStatus.NO_CONTENT) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  @RequestMapping(value = "/{userId:.+}/enrollments", method = RequestMethod.GET)
  public Collection<Enrollment> getEnrollmentsForUser(JwtAuthenticationToken token, @PathVariable("userId") final String userId) throws EnrollmentNotFoundException {
    UserContext userContext = (UserContext) token.getPrincipal();
    return enrollmentService.findEnrollmentsForUser(userContext.getTenantId(), userContext.getOrgId(), userId);
  }

  @RequestMapping(value = "/mapping/{externalUserId:.+}", method = RequestMethod.GET)
  public UserMapping getUserMapping(JwtAuthenticationToken token, @PathVariable("externalUserId") final String externalUserId) {
    UserContext userContext = (UserContext) token.getPrincipal();
    return mongoUserMappingRepository.findByTenantIdAndOrganizationIdAndUserExternalIdIgnoreCase(userContext.getTenantId(), userContext.getOrgId(), externalUserId);
  }

  @RequestMapping(value= "/mapping", method = RequestMethod.POST)
  public ResponseEntity<?> postUserMapping(JwtAuthenticationToken token, @RequestBody UserMapping um) {
    UserContext userContext = (UserContext) token.getPrincipal();

    UserMapping existingUserMapping = mongoUserMappingRepository
            .findByTenantIdAndOrganizationIdAndUserExternalIdIgnoreCase(userContext.getTenantId(), userContext.getOrgId(), um.getUserExternalId());

    if (existingUserMapping == null) {
      UserMapping userMapping
              = new UserMapping.Builder()
              .withUserExternalId(um.getUserExternalId())
              .withUserSourcedId(um.getUserSourcedId())
              .withDateLastModified(LocalDateTime.now(ZoneId.of("UTC")))
              .withOrganizationId(userContext.getOrgId())
              .withTenantId(userContext.getTenantId())
              .build();

      UserMapping saved = mongoUserMappingRepository.save(userMapping);

      return new ResponseEntity<>(saved, null, HttpStatus.CREATED);
    }

    return new ResponseEntity<>(existingUserMapping, null, HttpStatus.NOT_MODIFIED);

  }

  /** Returns the Result for user
   * @param token
   * @param userId
   * @return Result
   * @throws ResultNotFoundException
   */
  @RequestMapping(value = "/{userId:.+}/results", method = RequestMethod.GET)
  public Result getResultsForUser(JwtAuthenticationToken token, @PathVariable final String userId) throws EventNotFoundException, RuntimeException {
    UserContext userContext = (UserContext) token.getPrincipal();
    return resultService.getResultsForUser(userContext.getTenantId(), userContext.getOrgId(), userId);
  }

  @RequestMapping(value = "/{userId:.+}/events", method = RequestMethod.GET)
  public Collection<Event> getEventsForUser(
          JwtAuthenticationToken token,
          @PathVariable final String userId,
          @RequestParam(value="from", required=false, defaultValue = "") String from,
          @RequestParam(value="to", required=false, defaultValue = "") String to
  ) throws IllegalArgumentException, EventNotFoundException, BadRequestException {
    UserContext userContext = (UserContext) token.getPrincipal();
    try {
      return eventService.getEventsForUser(userContext.getTenantId(), userContext.getOrgId(), userId, from, to);
    } catch (EventNotFoundException e) {
      throw new EventNotFoundException(e.getMessage());
    } catch (BadRequestException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

}