package com.neueda.leap.service;

import com.neueda.leap.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getUsernameByClientIdDelegatesToMapper() {
        when(userMapper.findUsernameByClientId(42L)).thenReturn("alice");

        String result = userService.getUsernameByClientId(42L);

        assertEquals("alice", result);
        verify(userMapper).findUsernameByClientId(42L);
        verifyNoMoreInteractions(userMapper);
    }

    @Test
    void getUsernameByClientIdPassesNullThroughToMapper() {
        when(userMapper.findUsernameByClientId(null)).thenReturn(null);

        String result = userService.getUsernameByClientId(null);

        assertNull(result);
        verify(userMapper).findUsernameByClientId(null);
        verifyNoMoreInteractions(userMapper);
    }
}
