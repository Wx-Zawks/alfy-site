export const useInquiryDialog = () => {
  const isOpen = useState('inquiry-dialog-open', () => false)

  return {
    isOpen,
    open: () => { isOpen.value = true },
    close: () => { isOpen.value = false }
  }
}
