package com.user.User.Service.service;

import com.user.User.Service.repository.UserRepository;
import com.user.User.Service.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(1L, "mn3118110@gmail.com", "password", "Manjunath", "Gowda");
    }

    // ---- createUser tests ----

    @Test
    void createUser_ShouldReturnSavedUser() {
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = userService.createUser(user);

        assertNotNull(saved);
        assertEquals("mn3118110@gmail.com", saved.getEmail());
        assertEquals("Manjunath", saved.getFirstName());
        assertEquals("Gowda", saved.getLastName());
        verify(userRepository, times(1)).save(any(User.class));
    }

    // ---- getAllUsers tests ----

    @Test
    void getAllUsers_ShouldReturnListOfUsers() {
        User user2 = new User(2L, "ravi@gmail.com", "password", "Ravi", "Kumar");
        when(userRepository.findAll()).thenReturn(Arrays.asList(user, user2));

        List<User> users = userService.getAllUsers();

        assertEquals(2, users.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        when(userRepository.findAll()).thenReturn(List.of());

        List<User> users = userService.getAllUsers();

        assertTrue(users.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    // ---- getUserById tests ----

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> found = userService.getUserById(1L);

        assertTrue(found.isPresent());
        assertEquals("Manjunath", found.get().getFirstName());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<User> found = userService.getUserById(99L);

        assertFalse(found.isPresent());
        verify(userRepository, times(1)).findById(99L);
    }

    // ---- updateUser tests ----

    @Test
    void updateUser_WhenUserExists_ShouldReturnUpdatedUser() {
        User updatedDetails = new User(null, "updated@gmail.com", "newpass", "Manju Updated", "G");
        User updatedUser = new User(1L, "updated@gmail.com", "newpass", "Manju Updated", "G");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1L, updatedDetails);

        assertNotNull(result);
        assertEquals("Manju Updated", result.getFirstName());
        assertEquals("updated@gmail.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldThrowException() {
        User updatedDetails = new User(null, "ghost@gmail.com", "pass", "Ghost", "User");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(99L, updatedDetails));

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository, times(1)).findById(99L);
        verify(userRepository, never()).save(any(User.class));
    }

    // ---- deleteUser tests ----

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        doNothing().when(userRepository).delete(user);

        assertDoesNotThrow(() -> userService.deleteUser(1L));

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).delete(user);
    }

    @Test
    void deleteUser_WhenUserDoesNotExist_ShouldThrowException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.deleteUser(99L));

        assertEquals("User not found with id: 99", exception.getMessage());
        verify(userRepository, times(1)).findById(99L);
        verify(userRepository, never()).delete(any(User.class));
    }
}

