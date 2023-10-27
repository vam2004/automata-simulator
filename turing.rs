use std::collections::HashSet;
use std::fmt;
use std::sync::Arc;
use core::borrow::Borrow;
type State = isize;
type Symbol = char;
pub enum Direction {
    Left,
    Right,
}
impl fmt::Display for Direction {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{}",
            match &self {
                Direction::Left => "LEFT",
                Direction::Right => "RIGHT",
            }
        )
    }
}
#[allow(unused)]
pub struct Transition {
    pub from: State,
    pub goto: State,
    pub read: Symbol,
    pub write: Symbol,
    pub next: Direction,
}
impl Transition {
    #[allow(unused)]
    pub fn new(from: State, goto: State, read: Symbol, write: Symbol, next: Direction) -> Self {
        Self {
            from,
            goto,
            read,
            write,
            next,
        }
    }
}
impl fmt::Display for Transition {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(
            f,
            "{:0>4}: {} => {:0>4}: {}, {}",
            self.from, self.read, self.goto, self.write, self.next
        )
    }
}
/*
sintax:
    Symbol:
        It is any non-whitespace unicode character or a hexadecimal representation
        started with the preffix '0x'
    Label:
        It is a sequence of unicode character (including whitespaces)
        except both ';' and end-of-file characters. A label doesn't starts with the following
        sequences followed by whitespace:
        - 'move'
        - 'goto'
        - 'if'
        - 'write'
        - 'accept'
        - 'reject'
        If a block comment is present inside a label, the characters inside
        this comment are ignored.
    LabelDecraration:
        It is composed by a <Label> ended with ':' followed by a
        sequence of <Commands> separed by either ';' or end-of-line character.
        A label ends with the declaration of another label or end-of-file character.
    Direction:
        It is either 'left' or 'right'
    MoveCommand:
        It starts with 'move' followed by a <Direction>
    GotoCommand:
        It starts with 'goto' followed by a <Label>
    WriteCommand:
        It starts with 'write' followed by a <Symbol>
    EndCommand:
        It is either 'reject' or 'accept'
    IncoditionalBlock:
        It is either sequence of <MoveCommand> and <WriteCommand> commands,
        followed optionally by <GotoCommand>, or a <EndCommand>. It ends with
        either the ';' or end-of-file characters.
    IfCommand:
        It starts with 'if', followed optionally by 'not', and ended
        with a <Symbol> followed by <IncoditionalBlock>
*/
#[allow(unused)]
enum Token {
    If(Symbol),
    IfNot(Symbol),
    Write(Symbol),
    Move(Direction),
    Goto(Label),
}
#[allow(unused)]
pub struct LookAhead<T>
where
    T: Iterator,
{
    src: T,
    buffer: Vec<Option<T::Item>>,
    loaded: usize,
    cursor: usize,
}
impl<T> LookAhead<T>
where
    T: Iterator,
{
    pub fn new(src: T) -> Self {
        Self::with_vec(src, Vec::new())
    }
    pub fn with_capacity(src: T, size: usize) -> Self {
        Self::with_vec(src, Vec::with_capacity(size))
    }
    pub(crate) fn with_vec(src: T, buffer: Vec<Option<T::Item>>) -> Self {
        Self {
            src,
            buffer,
            loaded: 0, // the amount of cached items after the cursor
            cursor: 0, // the item to read from buffer
        }
    }
    pub fn clean(&mut self) {
        self.buffer.drain(..self.cursor);
        self.cursor = 0;
    }
    pub fn load(&mut self, offset: usize) {
        for index in self.loaded..offset {
            if let Some(data) = self.src.next() {
                self.buffer.push(Some(data));
            } else {
                self.loaded = index;
                return;
            }
        }
        if offset > self.loaded {
            self.loaded = offset;
        }
    }
    pub fn peek(&mut self, offset: usize) -> &Option<T::Item> {
        let cursor = self.cursor;
        let position = offset + cursor;
        /*println!(
            "cursor = {cursor}, position = {position}, loaded = {}",
            self.loaded
        );*/
        self.load(offset);
        // println!("[posload] loaded = {}", self.loaded);
        if offset > 0 && self.loaded >= offset {
            &self.buffer[position - 1]
        } else {
            &None
        }
    }
}
impl<T> Iterator for LookAhead<T>
where
    T: Iterator,
{
    type Item = T::Item;
    fn next(&mut self) -> Option<Self::Item> {
        let loaded = self.loaded;
        if self.cursor >= (loaded / 2) {
            self.clean();
        }
        let cursor = self.cursor;
        // println!("loaded = {loaded}");
        if loaded > 0 {
            self.cursor += 1;
            self.loaded -= 1;
            self.buffer[cursor].take()
        } else {
            self.src.next()
        }
    }
}
struct Tokenizer<T>
where
    T: Iterator<Item = char>,
{
    labels: LabelStorage,
    src: LookAhead<T>,
}
#[repr(transparent)] 
#[derive(PartialEq)]
#[derive(Hash)]
#[derive(core::cmp::Eq)]
#[derive(Debug)]
pub struct Label {
    inner: Arc<String>
}
impl Label {
    pub fn new<T: Into<String>>(src: T) -> Self {
        Self::from(src.into())
    }
    pub fn copies(&self) -> usize {
        Arc::strong_count(&self.inner)
    }
}
impl<T> From<T> for Label where T: Into<String> {
    fn from(src: T) -> Self {
        Self { inner: Arc::new(src.into()) }
    }
}
impl Borrow<str> for Label {
    fn borrow(&self) -> &str {
        &self.inner
    }
}
impl Clone for Label {
    fn clone(&self) -> Self {
        Self {
            inner: self.inner.clone()
        }
    }
}
#[repr(transparent)]
pub struct LabelStorage {
    pub cache: HashSet<Label>
}
impl LabelStorage {
    fn new() -> Self {
        Self { cache: HashSet::new() }
    }
    pub fn get_or_add<S: Into<Label> + Borrow<str>>(&mut self, src: S) -> Label {
        if let Some(label) = self.cache.get(src.borrow()) {
            label.clone()
        } else {
            let tmp = src.into();
            self.cache.insert(tmp.clone());
            tmp
        }
    }
    pub fn copies(&self, src: &str) -> Option<usize> {
        self.cache.get(src).and_then(|label| Some(label.copies() - 1))
    }
}
impl From<HashSet<Label>> for LabelStorage {
    fn from(cache: HashSet<Label>) -> Self {
        Self { cache }
    }
}
impl<T> Tokenizer<T>
where
    T: Iterator<Item = char>,
{
    pub fn new(src: T) -> Self {
        Self::with_buffers(LookAhead::new(src), HashSet::new().into())
    }
    pub fn with_buffers(src: LookAhead<T>, labels: LabelStorage) -> Self {
        Self { src, labels }
    }
    
}
impl<'a, T> Iterator for Tokenizer<T>
where
    T: Iterator<Item = char>,
{
    type Item = Token;
    fn next(&mut self) -> Option<Self::Item> {
        None
    }
}
fn main() {
    //let transition = Transition::new(0, 1, 'a', 'b', Direction::Left);
    let sample = r#"
simple /* not too much */ sample:
    if a write b move left goto simple sample /* comment here */
    if b move left write a goto simple sample // comment here
    if c move right write b goto simple sample 
    if /* comment here */ c /* comment here */
don't waste my time:
    reject
"#;
    let mut tokenizer = Tokenizer::new(sample.chars());
    //println!("{}", sample);
    //println!("{}", transition);
    // test_peek();
}

#[test]
fn test_label() {
    let mut storage = LabelStorage::new();
    let x = storage.get_or_add("hello_world".to_owned());
    let y = storage.get_or_add("hello_world");
    assert_eq!(storage.copies("hello_world"), Some(2));
    let z = storage.get_or_add(Label::new("hello_world"));
    assert_eq!(storage.copies("hello_world"), Some(3));
    assert_eq!(x, y);
    drop(x);
    assert_eq!(storage.copies("hello_world"), Some(2));
    assert_eq!(y, z);
}
#[test]
fn test_peek() {
    let tmp = [23, 7, 15, 6, 4, 26, 17, 9, 11, 2, 5, 35, 16];
    let it = tmp.iter().peekable();
    let mut ahead = LookAhead::new(it);
    assert_eq!(ahead.peek(0), &None);
    assert_eq!(ahead.src.peek(), Some(&&23));
    assert_eq!(ahead.peek(2), &Some(&7));
    assert_eq!(ahead.src.peek(), Some(&&15));
    assert_eq!(ahead.peek(7), &Some(&17));
    assert_eq!(ahead.src.peek(), Some(&&9));
    assert_eq!(ahead.peek(2), &Some(&7));
    assert_eq!(ahead.src.peek(), Some(&&9));

    assert_eq!(ahead.next(), Some(&23));

    assert_eq!(ahead.peek(1), &Some(&7));
    assert_eq!(ahead.peek(8), &Some(&11));
    assert_eq!(ahead.peek(6), &Some(&17));
    assert_eq!(ahead.src.peek(), Some(&&2));

    for i in 1..10 {
        assert_eq!(ahead.peek(1), &Some(&tmp[i]));
        assert_eq!(ahead.next(), Some(&tmp[i]));
    }

    assert_eq!(ahead.peek(1), &Some(&5));
    assert_eq!(ahead.next(), Some(&5));
    assert_eq!(ahead.next(), Some(&35));
    assert_eq!(ahead.peek(1), &Some(&16));
    assert_eq!(ahead.peek(2), &None);
}
