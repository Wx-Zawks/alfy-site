import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RichTextEditor from '../rich-text-editor.vue';

describe('rich text editor managed media previews', () => {
  it('renders a preview URL but emits the persistent media reference', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        mediaPreviewUrls: { 2: 'blob:http://localhost:5777/media-2' },
        modelValue:
          '<figure><img src="alfy-media:2" alt="产品图" loading="lazy"></figure>',
      },
    });

    const editor = wrapper.get('.rich-text-content');
    const image = editor.get('img');
    expect(image.attributes('src')).toBe('blob:http://localhost:5777/media-2');
    expect(image.attributes('data-alfy-media-id')).toBe('2');

    image.element.setAttribute('alt', '更新后的产品图');
    await editor.trigger('input');

    const updates = wrapper.emitted('update:modelValue');
    const html = String(updates?.at(-1)?.[0] || '');
    expect(html).toContain('src="alfy-media:2"');
    expect(html).toContain('alt="更新后的产品图"');
    expect(html).not.toContain('blob:');
    expect(html).not.toContain('data-alfy-media-id');
  });

  it('refreshes the rendered image when its preview URL changes', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        mediaPreviewUrls: { 2: 'blob:http://localhost:5777/old-media-2' },
        modelValue: '<img src="alfy-media:2" alt="产品图">',
      },
    });

    await wrapper.setProps({
      mediaPreviewUrls: { 2: 'blob:http://localhost:5777/new-media-2' },
    });

    expect(wrapper.get('.rich-text-content img').attributes('src')).toBe(
      'blob:http://localhost:5777/new-media-2',
    );
  });
});
