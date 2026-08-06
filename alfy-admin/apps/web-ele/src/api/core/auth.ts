import { baseRequestClient, requestClient } from '#/api/request';

export namespace AuthApi {
  /** 登录接口参数 */
  export interface LoginParams {
    password?: string;
    username?: string;
  }

  /** 登录接口返回值 */
  export interface LoginResult {
    accessToken: string;
    expiresIn: number;
    refreshToken: string;
    tokenType: string;
    user: {
      id: number;
      role: string;
      username: string;
    };
  }

  export interface ApiEnvelope<T> {
    code: number;
    data: T;
    message: string;
  }
}

const REFRESH_TOKEN_KEY = 'alfy-admin-refresh-token';

export function clearRefreshToken() {
  localStorage.removeItem(REFRESH_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || '';
}

export function setRefreshToken(value: string) {
  localStorage.setItem(REFRESH_TOKEN_KEY, value);
}

/**
 * 登录
 */
export async function loginApi(data: AuthApi.LoginParams) {
  return requestClient.post<AuthApi.LoginResult>('/admin/auth/login', data);
}

/**
 * 刷新accessToken
 */
export async function refreshTokenApi() {
  const refreshToken = getRefreshToken();
  if (!refreshToken) throw new Error('缺少刷新令牌，请重新登录');

  const response = await baseRequestClient.post<
    AuthApi.ApiEnvelope<AuthApi.LoginResult>
  >('/admin/auth/refresh', { refreshToken });
  const envelope = (
    response as unknown as {
      data: AuthApi.ApiEnvelope<AuthApi.LoginResult>;
    }
  ).data;
  if (envelope.code !== 0)
    throw new Error(envelope.message || '刷新登录状态失败');
  setRefreshToken(envelope.data.refreshToken);
  return envelope.data;
}

/**
 * 退出登录
 */
export async function logoutApi() {
  const refreshToken = getRefreshToken();
  try {
    if (refreshToken) {
      await requestClient.post('/admin/auth/logout', { refreshToken });
    }
  } finally {
    clearRefreshToken();
  }
}

/**
 * 获取用户权限码
 */
export async function getAccessCodesApi() {
  return requestClient.get<string[]>('/admin/auth/codes');
}
