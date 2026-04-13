package com.user.User.Service.controller;

import com.user.User.Service.dto.UserRequestDto;
import com.user.User.Service.service.UserService;
import com.user.User.Service.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User user;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {

        user = new User(1L, "mn3118110@gmail.com", "password", "Manjunath", "Gowda");
        userRequestDto =new UserRequestDto("mn3118110@gmail.com", "password", "Manjunath", "Gowda");
    }

    // ---- POST /users ----

    @Test
    void createUser_ShouldReturnCreatedUser() {
        when(userService.createUser(any(UserRequestDto.class))).thenReturn(user);

        ResponseEntity<User> response = userController.createUser(userRequestDto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Manjunath", response.getBody().getFirstName());
        assertEquals("Gowda", response.getBody().getLastName());
        assertEquals("mn3118110@gmail.com", response.getBody().getEmail());
        verify(userService, times(1)).createUser(any(UserRequestDto.class));
    }

    // ---- GET /users ----

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        List<User> users = Arrays.asList(
                user,
                new User(2L, "ravi@gmail.com", "password", "Ravi", "Kumar")
        );
        when(userService.getAllUsers()).thenReturn(users);

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        verify(userService, times(1)).getAllUsers();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userService.getAllUsers()).thenReturn(List.of());

        ResponseEntity<List<User>> response = userController.getAllUsers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // ---- GET /users/{id} ----

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        when(userService.getUserById(1L)).thenReturn(Optional.of(user));

        ResponseEntity<User> response = userController.getUserById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Manjunath", response.getBody().getFirstName());
        verify(userService, times(1)).getUserById(1L);
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturn404() {
        when(userService.getUserById(99L)).thenReturn(Optional.empty());

        ResponseEntity<User> response = userController.getUserById(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).getUserById(99L);
    }

    // ---- PUT /users/{id} ----

    @Test
    void updateUser_WhenUserExists_ShouldReturnUpdatedUser() {
        User updatedUser = new User(1L, "updated@gmail.com", "newpass", "Manju Updated", "G");
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(updatedUser);

        ResponseEntity<User> response = userController.updateUser(1L, updatedUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Manju Updated", response.getBody().getFirstName());
        assertEquals("updated@gmail.com", response.getBody().getEmail());
        verify(userService, times(1)).updateUser(eq(1L), any(User.class));
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldReturn404() {
        User updatedUser = new User(null, "ghost@gmail.com", "pass", "Ghost", "User");
        when(userService.updateUser(eq(99L), any(User.class)))
                .thenThrow(new RuntimeException("User not found with id: 99"));

        ResponseEntity<User> response = userController.updateUser(99L, updatedUser);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).updateUser(eq(99L), any(User.class));
    }

    // ---- DELETE /users/{id} ----

    @Test
    void deleteUser_WhenUserExists_ShouldReturn204() {
        doNothing().when(userService).deleteUser(1L);

        ResponseEntity<Void> response = userController.deleteUser(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService, times(1)).deleteUser(1L);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldReturn404() {
        doThrow(new RuntimeException("User not found with id: 99"))
                .when(userService).deleteUser(99L);

        ResponseEntity<Void> response = userController.deleteUser(99L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).deleteUser(99L);
    }
}

