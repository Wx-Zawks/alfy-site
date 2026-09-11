import { mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';

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

async function applyAlignmentToLooseText(
  wrapper: ReturnType<typeof mount>,
  text: string,
  value: 'justifyCenter' | 'justifyLeft' | 'justifyRight',
) {
  const editor = wrapper.get('.rich-text-content').element;
  const textNode = [...editor.childNodes].find(
    (node) => node.nodeType === Node.TEXT_NODE && node.textContent?.includes(text),
  );
  if (!textNode?.textContent) throw new Error(`找不到裸文本：${text}`);

  const start = textNode.textContent.indexOf(text);
  const range = document.createRange();
  range.setStart(textNode, start);
  range.setEnd(textNode, start + text.length);
  const selection = window.getSelection();
  selection?.removeAllRanges();
  selection?.addRange(range);
  document.dispatchEvent(new Event('selectionchange'));

  await wrapper
    .get('select[aria-label="对齐方式"]')
    .setValue(value);
}

describe('rich text editor managed media previews', () => {
  it('removes legacy image placeholders that have no source address', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<p>正文开头</p><p><img>&nbsp;</p><figure><img src="alfy-media:2" alt="现场图"></figure>',
      },
    });

    await wrapper.vm.$nextTick();

    const html = wrapper.get('.rich-text-content').html();
    expect(html).not.toContain('<img>');
    expect(html).toContain('src="alfy-media:2"');
    expect(wrapper.emitted('update:modelValue')?.at(-1)?.[0]).not.toContain(
      '<img>',
    );
  });

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

  it('preserves the selected paragraph when toolbar focus collapses selection', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue: '<p>项目实拍①</p><p>项目实拍②</p>',
      },
    });
    const originalFocus = HTMLElement.prototype.focus;
    const focusSpy = vi
      .spyOn(HTMLElement.prototype, 'focus')
      .mockImplementation(function (this: HTMLElement) {
        originalFocus.call(this);
        if (!this.classList.contains('rich-text-content')) return;

        const firstParagraph = this.querySelector('p');
        if (!firstParagraph) return;
        const range = document.createRange();
        range.selectNodeContents(firstParagraph);
        range.collapse(true);
        const selection = window.getSelection();
        selection?.removeAllRanges();
        selection?.addRange(range);
        document.dispatchEvent(new Event('selectionchange'));
      });

    try {
      await applyAlignment(wrapper, 'p:nth-of-type(2)', 'justifyCenter');
    } finally {
      focusSpy.mockRestore();
    }

    expect(
      wrapper.get('.rich-text-content p:nth-of-type(1)').attributes(
        'data-align',
      ),
    ).toBeUndefined();
    expect(
      wrapper.get('.rich-text-content p:nth-of-type(2)').attributes(
        'data-align',
      ),
    ).toBe('center');
  });

  it('wraps a selected loose text line before applying alignment', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<div data-align="center">项目实拍①</div><br>项目实拍②<br><figure><img src="/image.jpg" alt=""></figure>',
      },
    });

    await applyAlignmentToLooseText(wrapper, '项目实拍②', 'justifyCenter');

    const editor = wrapper.get('.rich-text-content');
    const paragraphs = editor.findAll('p');
    expect(editor.get('div').attributes('data-align')).toBe('center');
    expect(paragraphs[0]?.text()).toBe('项目实拍②');
    expect(paragraphs[0]?.attributes('data-align')).toBe('center');
    expect(editor.html()).toContain('<br>');
    expect(editor.get('figure img').attributes('src')).toBe('/image.jpg');
  });

  it('splits a legacy wrapper so aligning one title cannot align figures', async () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<div data-align="center"><p>项目实拍①</p><br><p>项目实拍②</p><br><figure><img src="/first.jpg" alt="第一张"></figure><br><p>项目实拍③</p><figure><img src="/second.jpg" alt="第二张"></figure></div>',
      },
    });

    await applyAlignment(wrapper, 'p:nth-of-type(3)', 'justifyRight');

    const editor = wrapper.get('.rich-text-content');
    const paragraphs = editor.findAll('p');
    expect(editor.findAll('div')).toHaveLength(0);
    expect(paragraphs[0]?.attributes('data-align')).toBe('center');
    expect(paragraphs[1]?.attributes('data-align')).toBeUndefined();
    expect(paragraphs[2]?.attributes('data-align')).toBe('right');
    expect(editor.findAll('figure')).toHaveLength(2);
    expect(editor.get('figure:nth-of-type(1) img').attributes('src')).toBe(
      '/first.jpg',
    );
    expect(editor.get('figure:nth-of-type(2) img').attributes('src')).toBe(
      '/second.jpg',
    );
  });

  it('converts labels and nested media figures into independent blocks', () => {
    const wrapper = mount(RichTextEditor, {
      props: {
        modelValue:
          '<div><figure><b>项目实拍②</b></figure><figure><figure><img src="/second.jpg" alt="第二张"></figure><figure><b>项目实拍③</b></figure><figure><figure><img src="/third.jpg" alt="第三张"></figure></figure></figure></div>',
      },
    });

    const editor = wrapper.get('.rich-text-content');
    expect(editor.findAll('div')).toHaveLength(0);
    expect(editor.findAll('figure')).toHaveLength(2);
    expect(editor.findAll('p')).toHaveLength(2);
    expect(editor.findAll('p')[0]?.text()).toBe('项目实拍②');
    expect(editor.findAll('p')[1]?.text()).toBe('项目实拍③');
    expect(editor.get('figure:nth-of-type(1) img').attributes('src')).toBe(
      '/second.jpg',
    );
    expect(editor.get('figure:nth-of-type(2) img').attributes('src')).toBe(
      '/third.jpg',
    );
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
