using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using static System.Windows.Forms.VisualStyles.VisualStyleElement;

namespace APPSDEV_PROJECT
{
    public partial class Details : Form
    {
        // Email is passed in from Form2 — no TextBox needed here
        private string userEmail;

        public string receivedName { get; private set; }
        public string receivedLocation { get; private set; }
        public string receivedroomType { get; private set; }
        public string receivedReview { get; private set; }
        public int receivedPrice { get; private set; }
        public double receivedScore { get; private set; }
        public int receivedReviewNum { get; private set; }
        public int receivedProduct { get; private set; }

        public int receivedtax { get; private set; }
        public int receivedtotal { get; private set; }


        public Details(string hotelName, string location, string roomType, int tax, string review, int price, double score, int reviewNum, int product, int total, string email = "")
        {
            InitializeComponent();
            receivedName = hotelName;
            receivedLocation = location;
            receivedroomType = roomType;
            receivedPrice = price;
            receivedScore = score;
            receivedReviewNum = reviewNum;
            receivedReview = review;
            receivedProduct = product;
            receivedtax = tax;
            receivedtotal = total;
            userEmail = email; // store the email passed from Form2
        }


        private void btn_select_Click(object sender, EventArgs e)
        {
            this.Hide();
            Form3 f3 = new Form3();
            Purchase p = new Purchase(this, userEmail, f3); // use stored email
            p.ShowDialog();
            this.Show();

            

        }

        private void btn_cancel_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void Details_Load_1(object sender, EventArgs e)
        {
            lbl_score.Text = receivedScore.ToString();//Score
            lbl_reviewNum.Text = receivedReviewNum.ToString() + "reviews";//Reviews
            lbl_review.Text = receivedReview.ToString();//katong AMAZING
            lbl_price.Text = "₱" + receivedPrice.ToString() + " nightly";//Price ni dire

            int p = receivedProduct;

            //pb_image.Image = Image.FromFile(@"C:\Users\Entice\source\repos\APPSDEV-PROJECT\IMAGES\Product\p1.jpg");
            //DALI RANI MA GUBA , MAG BUHAT NA LNG KOG DATA BASE PARA SA KANING IMAGES PARA SA DETAILS FOR EACH PRODUCT
            // using if and else if statement 
            //p = 0 to 3; 


        }
    }
}
