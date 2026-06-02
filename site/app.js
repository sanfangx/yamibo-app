const slides = [
  {
    src: "site/assets/screenshots/home.png",
    alt: "首頁畫面",
    caption: "首頁：分類、版塊與今日更新。"
  },
  {
    src: "site/assets/screenshots/forum.png",
    alt: "版面畫面",
    caption: "版面：置頂、公告、排序、分類與帖子列表。"
  },
  {
    src: "site/assets/screenshots/reader.png",
    alt: "閱讀器畫面",
    caption: "閱讀器：帖子內容、評分、回覆與標籤入口。"
  },
  {
    src: "site/assets/screenshots/history.png",
    alt: "閱讀紀錄畫面",
    caption: "閱讀紀錄：搜尋、篩選、分頁與快速回讀。"
  },
  {
    src: "site/assets/screenshots/favorite.png",
    alt: "收藏畫面",
    caption: "收藏：分類、集合、排序與同步。"
  },
  {
    src: "site/assets/screenshots/message.png",
    alt: "消息中心畫面",
    caption: "消息：收藏更新、私訊與提醒。"
  },
  {
    src: "site/assets/screenshots/profile.png",
    alt: "個人頁畫面",
    caption: "我的：登入狀態、每日簽到、設定與閱讀統計。"
  }
];

document.documentElement.classList.add("js");

const header = document.querySelector(".site-header");
const navLinks = [...document.querySelectorAll(".top-nav a")];
const sections = navLinks
  .map((link) => document.querySelector(link.getAttribute("href")))
  .filter(Boolean);

function setHeaderState() {
  header.dataset.elevated = window.scrollY > 12 ? "true" : "false";
}

function updateActiveNav() {
  const current = sections.findLast((section) => section.offsetTop <= window.scrollY + 120);
  navLinks.forEach((link) => {
    link.classList.toggle("is-active", current && link.getAttribute("href") === `#${current.id}`);
  });
}

setHeaderState();
updateActiveNav();
window.addEventListener("scroll", () => {
  setHeaderState();
  updateActiveNav();
}, { passive: true });

const revealObserver = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      entry.target.classList.add("is-visible");
      revealObserver.unobserve(entry.target);
    }
  });
}, { threshold: 0.16 });

document.querySelectorAll(".reveal").forEach((element) => revealObserver.observe(element));

const carouselImage = document.querySelector("[data-carousel-image]");
const carouselCaption = document.querySelector("[data-carousel-caption]");
const thumbButtons = [...document.querySelectorAll("[data-slide]")];
let activeSlide = 0;

function renderSlide(index) {
  activeSlide = (index + slides.length) % slides.length;
  const slide = slides[activeSlide];
  carouselImage.src = slide.src;
  carouselImage.alt = slide.alt;
  carouselCaption.textContent = slide.caption;
  thumbButtons.forEach((button) => {
    button.classList.toggle("is-active", Number(button.dataset.slide) === activeSlide);
  });
}

document.querySelector("[data-carousel-prev]").addEventListener("click", () => renderSlide(activeSlide - 1));
document.querySelector("[data-carousel-next]").addEventListener("click", () => renderSlide(activeSlide + 1));
thumbButtons.forEach((button) => {
  button.addEventListener("click", () => renderSlide(Number(button.dataset.slide)));
});

renderSlide(0);
