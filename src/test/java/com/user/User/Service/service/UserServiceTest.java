package com.user.User.Service.service;

import com.user.User.Service.dto.UserRequestDto;
import com.user.User.Service.repository.UserRepository;
import com.user.User.Service.user.User;
import com.user.User.Service.util.Utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.modelmapper.ModelMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private Utils utils;

    @Mock
    private ModelMapper modelMapper;

    private UserService userService;

    private User user;
    private UserRequestDto userRequestDto;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, utils);
        user = new User(1L, "mn3118110@gmail.com", "password", "Manjunath", "Gowda");
        userRequestDto = new UserRequestDto("mn3118110@gmail.com", "password", "Manjunath", "Gowda");
    }

    // ---- createUser tests ----

    @Test
    void createUser_ShouldReturnSavedUser() {
        User mappedUser = new User(null, "mn3118110@gmail.com", "password", "Manjunath", "Gowda");

        when(utils.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(any(UserRequestDto.class), eq(User.class))).thenReturn(mappedUser);
        when(userRepository.save(any(User.class))).thenReturn(user);

        User saved = userService.createUser(userRequestDto);

        assertNotNull(saved);
        assertEquals("mn3118110@gmail.com", saved.getEmail());
        assertEquals("Manjunath", saved.getFirstName());
        assertEquals("Gowda", saved.getLastName());
        verify(utils, times(1)).getMapper();
        verify(modelMapper, times(1)).map(any(UserRequestDto.class), eq(User.class));
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
        UserRequestDto updateRequest = new UserRequestDto("updated@gmail.com", "newpass", "Manju Updated", "G");
        User mappedDetails = new User(null, "updated@gmail.com", "newpass", "Manju Updated", "G");
        User updatedUser = new User(1L, "updated@gmail.com", "newpass", "Manju Updated", "G");

        when(utils.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(any(UserRequestDto.class), eq(User.class))).thenReturn(mappedDetails);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userService.updateUser(1L, updateRequest);

        assertNotNull(result);
        assertEquals("Manju Updated", result.getFirstName());
        assertEquals("updated@gmail.com", result.getEmail());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_WhenUserDoesNotExist_ShouldThrowException() {
        UserRequestDto updateRequest = new UserRequestDto("ghost@gmail.com", "pass", "Ghost", "User");
        User mappedDetails = new User(null, "ghost@gmail.com", "pass", "Ghost", "User");

        when(utils.getMapper()).thenReturn(modelMapper);
        when(modelMapper.map(any(UserRequestDto.class), eq(User.class))).thenReturn(mappedDetails);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.updateUser(99L, updateRequest));

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

