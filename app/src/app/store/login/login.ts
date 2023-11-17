import {CurrentUser, LoginState} from "../../../types";
import {createSlice, PayloadAction} from "@reduxjs/toolkit";
import {actions} from "./loginThunks";
import {validateToken} from "../../../helpers";
import {CURRENT_USER, saveCurrentUser} from "../../../localStorageManager";

export const initialLoginState: LoginState = {
    user: {} as CurrentUser,
    userError: null,
    isAuthenticated: false,
    authConfig: {},
    authConfigError: null
};

export const loginSlice = createSlice({
    name: "login",
    initialState: initialLoginState,
    reducers: {
        logout: (state) => {
            state.user = {} as CurrentUser;
            state.isAuthenticated = false;
            localStorage.removeItem(CURRENT_USER);
        },
        saveUser: (state, action: PayloadAction<CurrentUser>) => {
            state.user = action.payload;
            saveCurrentUser(state.user);
            state.isAuthenticated = validateToken(state.user);
            state.userError = null;
        },
        loginError: (state, action: PayloadAction<string>) => {
            state.userError = {
                error: {
                    error: "Login failed",
                    detail: action.payload
                }
            };
        }
    },
    extraReducers: (builder) => {
        builder
            .addCase(actions.fetchToken.fulfilled, (state, action) => {
                state.user = action.payload;
                saveCurrentUser(state.user);
                state.isAuthenticated = validateToken(state.user);
                state.userError = null;
            })
            .addCase(actions.fetchToken.rejected, (state, action) => {
                state.userError = action.payload ?? null;
            })
            .addCase(actions.fetchAuthConfig.fulfilled, (state, action) => {
                state.authConfig = action.payload;
                state.authConfigError = null;
            })
            .addCase(actions.fetchAuthConfig.rejected, (state, action) => {
                state.authConfigError = action.payload ?? null;
            });
    }
});

export const {
    logout,
    saveUser,
    loginError
} = loginSlice.actions;

export default loginSlice.reducer;
