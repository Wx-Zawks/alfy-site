import { mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';

import RichTextEditor from '../rich-text-editor.vue';

async function applyAlignment(
  wrapper: ReturnType<typeof mount>,
  selector: string,
  value: 'justifyCenter' | 'justifyLeft' | 'justifyRight',
) {
  const target = wrapper.get(selector).element;
  const range = document.createRange();
  range.selectNodeContents(target);
  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
  document.dispatchEvent(new Event('selectionchange'));

  await wrapper
    .get('select[aria-label="对齐方式"]')
    .setValue(value);
}

describe('rich text editor managed media previews', () => {
  it('requests a managed video from the toolbar', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        mediaVideoPicker: true,
        showVideoButton: true,
      },
    });

    await wrapper.get('button[aria-label="插入视频"]').trigger('click');

    expect(wrapper.emitted('requestVideo')).toHaveLength(1);
  });

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

  it('keeps alignment independent for consecutive rich-text paragraphs', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<p>项目实拍①</p><p>项目实拍②</p><p>项目实拍③&nbsp;</p>',
      },
    });

    await applyAlignment(wrapper, 'p:nth-of-type(2)', 'justifyCenter');
    await applyAlignment(wrapper, 'p:nth-of-type(3)', 'justifyCenter');

    const paragraphs = wrapper.findAll('.rich-text-content p');
    expect(paragraphs[0]?.attributes('data-align')).toBeUndefined();
    expect(paragraphs[1]?.attributes('data-align')).toBe('center');
    expect(paragraphs[2]?.attributes('data-align')).toBe('center');

    const html = String(wrapper.emitted('update:modelValue')?.at(-1)?.[0]);
    expect(html).toContain('<p data-align="center">项目实拍②</p>');
    expect(html).toContain('data-align="center">项目实拍③&nbsp;</p>');
  });

  it('applies an alignment only to the selected blocks', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<h2 data-align="center">已有标题</h2><p>第一段</p><p>第二段</p>',
      },
    });

    await applyAlignment(wrapper, 'p:nth-of-type(1)', 'justifyRight');

    expect(
      wrapper.get('.rich-text-content h2').attributes('data-align'),
    ).toBe('center');
    expect(
      wrapper.get('.rich-text-content p:nth-of-type(1)').attributes(
        'data-align',
      ),
    ).toBe('right');
    expect(
      wrapper.get('.rich-text-content p:nth-of-type(2)').attributes(
        'data-align',
      ),
    ).toBeUndefined();
  });
});
