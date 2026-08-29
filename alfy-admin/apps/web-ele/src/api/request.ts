/**
 * 该文件可自行根据业务逻辑进行调整
 */
import type { HttpResponse } from '@vben/request';

import { useAppConfig } from '@vben/hooks';
import { preferences } from '@vben/preferences';
import {
  authenticateResponseInterceptor,
  errorMessageResponseInterceptor,
  RequestClient,
} from '@vben/request';
import { useAccessStore } from '@vben/stores';

import { ElMessage } from 'element-plus';

import { useAuthStore } from '#/store';

import { clearRefreshToken, refreshTokenApi } from './core';

// 使用标准 Vite 环境变量，部署时由 .env.production 或服务器构建参数提供。
const { apiURL: configuredApiURL } = useAppConfig(import.meta.env, false);
const apiURL = configuredApiURL || '/api/v1';

function apiErrorPayload(error: unknown): unknown {
  const value = error as {
    data?: unknown;
    response?: { data?: unknown };
  };
  return value?.response?.data ?? value?.data ?? error;
}

function messageFromPayload(payload: unknown) {
  if (!payload || typeof payload !== 'object') return '';
  const value = payload as { error?: unknown; message?: unknown };
  const message = value.error ?? value.message;
  return typeof message === 'string' ? message.trim() : '';
}

function parseErrorText(value: string): unknown {
  const text = value.trim();
  if (!text) return '';
  try {
    return JSON.parse(text);
  } catch {
    return text.length <= 300 && !/<(?:html|body)[\s>]/i.test(text) ? text : '';
  }
}

export function getApiErrorCode(error: unknown) {
  const payload = apiErrorPayload(error);
  if (!payload || typeof payload !== 'object') return undefined;
  const code = Number((payload as { code?: unknown }).code);
  return Number.isFinite(code) ? code : undefined;
}

export async function getApiErrorMessage(
  error: unknown,
  fallback = '请求失败，请稍后重试',
) {
  let payload = apiErrorPayload(error);
  if (typeof Blob !== 'undefined' && payload instanceof Blob) {
    payload = parseErrorText(await payload.text());
  } else if (typeof payload === 'string') {
    payload = parseErrorText(payload);
  }

  const apiMessage = messageFromPayload(payload);
  if (apiMessage) return apiMessage;
  if (typeof payload === 'string' && payload) return payload;

  const directMessage =
    error instanceof Error && error.message ? error.message.trim() : '';
  if (/network error/i.test(directMessage)) {
    return '网络连接异常，请检查网络后重试';
  }
  if (/timeout/i.test(directMessage)) return '请求超时，请稍后重试';
  return directMessage || fallback;
}

export async function showApiErrorMessage(error: unknown, fallback?: string) {
  const message = await getApiErrorMessage(error, fallback);
  ElMessage.error({ grouping: true, message });
  return message;
}

function createRequestClient(
  baseURL: string,
  options: { enableAuthentication?: boolean } = {},
) {
  const { enableAuthentication = true } = options;
  const client = new RequestClient({
    baseURL,
  });

  /**
   * 重新认证逻辑
   */
  async function doReAuthenticate() {
    console.warn('Access token or refresh token is invalid or expired. ');
    const accessStore = useAccessStore();
    const authStore = useAuthStore();
    accessStore.setAccessToken(null);
    clearRefreshToken();
    if (
      preferences.app.loginExpiredMode === 'modal' &&
      accessStore.isAccessChecked
    ) {
      accessStore.setLoginExpired(true);
    } else {
      await authStore.logout();
    }
  }

  /**
   * 刷新token逻辑
   */
  async function doRefreshToken() {
    const accessStore = useAccessStore();
    const resp = await refreshTokenApi();
    const newToken = resp.accessToken;
    accessStore.setAccessToken(newToken);
    return newToken;
  }

  function formatToken(token: null | string) {
    return token ? `Bearer ${token}` : null;
  }

  // 请求头处理
  client.addRequestInterceptor({
    fulfilled: async (config) => {
      const accessStore = useAccessStore();

      config.headers.Authorization = formatToken(accessStore.accessToken);
      config.headers['Accept-Language'] = preferences.app.locale;
      return config;
    },
  });

  // response数据解构
  client.addResponseInterceptor<HttpResponse>({
    fulfilled: (response) => {
      const { data: responseData, status } = response;

      const { code, data } = responseData;
      if (status >= 200 && status < 400 && code === 0) {
        return data;
      }
      throw Object.assign({}, response, { response });
    },
  });

  // 登录请求返回 401 时代表凭据错误，不应触发刷新 token 或重新登录流程。
  if (enableAuthentication) {
    client.addResponseInterceptor(
      authenticateResponseInterceptor({
        client,
        doReAuthenticate,
        doRefreshToken,
        enableRefreshToken: preferences.app.enableRefreshToken,
        formatToken,
      }),
    );
  }

  // 通用的错误处理,如果没有进入上面的错误处理逻辑，就会进入这里
  client.addResponseInterceptor(
    errorMessageResponseInterceptor((msg: string, error) => {
      // 这里可以根据业务进行定制,你可以拿到 error 内的信息进行定制化处理，根据不同的 code 做不同的提示，而不是直接使用 message.error 提示 msg
      // 当前mock接口返回的错误字段是 error 或者 message
      const responseData = error?.response?.data ?? {};
      const responseMessage =
        responseData?.error ?? responseData?.message ?? '';
      // 基础客户端会将刷新接口的错误响应体直接抛出，此时 message 位于顶层。
      const directApiMessage = Number.isFinite(Number(error?.code))
        ? error?.message
        : '';
      const errorMessage = responseMessage || directApiMessage;
      // 如果没有错误信息，则会根据状态码进行提示
      ElMessage.error({ grouping: true, message: errorMessage || msg });
    }),
  );

  return client;
}

export const requestClient = createRequestClient(apiURL);

// 登录接口使用独立客户端，保留后端返回的“用户名或密码错误”等具体信息。
export const loginRequestClient = createRequestClient(apiURL, {
  enableAuthentication: false,
});

export const baseRequestClient = new RequestClient({ baseURL: apiURL });

export const rawRequestClient = new RequestClient({ baseURL: apiURL });
rawRequestClient.addRequestInterceptor({
  fulfilled: async (config) => {
    const accessStore = useAccessStore();
    config.headers.Authorization = formatRawToken(accessStore.accessToken);
    return config;
  },
});

function formatRawToken(token: null | string) {
  return token ? `Bearer ${token}` : undefined;
}
